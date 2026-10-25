#include "settings_sync_glue.h"

#include "generated/protocol.h"
#include "revision.h"
#include "theme.h"

// The settings revision (0–99), moved on by each edit made on the watch (REQ-WATCH-025).
#define PERSIST_KEY_SETTINGS_REVISION 28

static int s_revision;
static void (*s_request_send)(void);

static void store_revision(int revision) {
  s_revision = revision;
  const int written = persist_write_int(PERSIST_KEY_SETTINGS_REVISION, revision);
  if (written < 0) {
    // Not fatal: this session goes on with the revision in memory; the next edit writes it again.
    APP_LOG(APP_LOG_LEVEL_WARNING, "settings revision not persisted (%d)", written);
  }
}

void settings_sync_init(void (*request_send)(void)) {
  s_request_send = request_send;
  if (persist_exists(PERSIST_KEY_SETTINGS_REVISION)) {
    const int stored = persist_read_int(PERSIST_KEY_SETTINGS_REVISION);
    s_revision = revision_valid(stored) ? stored : 0;
  }
  settings_set_edit_handler(settings_sync_on_watch_edit);
}

void settings_sync_on_watch_edit(void) {
  store_revision(revision_next(s_revision));
  if (s_request_send != NULL) {
    s_request_send();
  }
}

void settings_sync_write(DictionaryIterator *out) {
  dict_write_uint8(out, PBNTN_KEY_SETTINGS_REVISION, (uint8_t)s_revision);
  dict_write_uint8(out, PBNTN_KEY_WATCH_PLATFORM, sync_build_platform());
  dict_write_uint8(out, PBNTN_KEY_WATCH_CAPABILITIES, sync_build_caps());
  for (int i = 0; i < SET_COUNT; i++) {
    dict_write_uint8(out, PBNTN_KEY_SETTING_ACCENT + i, (uint8_t)settings_get((SettingId)i));
  }
}

// A 32-bit integer tuple as an int, which is how the phone sends every number (PebbleKit Int32);
// -1 when the tuple is missing or of another type or width, so it is ignored.
static int tuple_int(const Tuple *t) {
  if (t == NULL || t->type == TUPLE_CSTRING || t->type == TUPLE_BYTE_ARRAY || t->length != 4) {
    return -1;
  }
  return (int)t->value->int32;
}

// The value the message carries for `id` when it is one this watch takes, else -1.
static int incoming(DictionaryIterator *in, SettingId id, uint32_t caps) {
  const int value = tuple_int(dict_find(in, PBNTN_KEY_SETTING_ACCENT + id));
  return (value >= 0 && sync_valid(id, value, caps)) ? value : -1;
}

SyncDecision settings_sync_apply(DictionaryIterator *in) {
  const uint32_t caps = sync_build_caps();
  const int phone_revision = tuple_int(dict_find(in, PBNTN_KEY_SETTINGS_REVISION));
  bool differs = false;
  for (int i = 0; i < SET_COUNT && !differs; i++) {
    const int value = incoming(in, (SettingId)i, caps);
    differs = value >= 0 && value != settings_get((SettingId)i);
  }
  const SyncDecision decision = sync_decide(s_revision, phone_revision, differs);
  if (decision == SYNC_APPLY_PHONE) {
    for (int i = 0; i < SET_COUNT; i++) {
      const int value = incoming(in, (SettingId)i, caps);
      if (value >= 0) {
        settings_store((SettingId)i, value);  // writes only a changed value
      }
    }
    store_revision(phone_revision);
  }
  return decision;
}
