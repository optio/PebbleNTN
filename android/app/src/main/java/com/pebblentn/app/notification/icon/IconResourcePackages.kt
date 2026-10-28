package com.pebblentn.app.notification.icon

import com.pebblentn.app.catalog.RuleFamilies

/**
 * The resource package names to look a turn drawable up under, in order, for a notification posted by
 * [postingPackage] (#113 follow-up): the posting package itself, then, for an app that uses another
 * app's rules (MicroG Maps), that family's root package ([RuleFamilies.resourceFallbackFor]). A
 * re-packaged build may keep the root app's resource package name. The lookup always stays in the
 * posting app's own context; the root is only a name, never a context to open. Pure.
 */
fun resourcePackagesToTry(postingPackage: String, families: RuleFamilies): List<String> =
    listOf(postingPackage) + listOfNotNull(families.resourceFallbackFor(postingPackage))
