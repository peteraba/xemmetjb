# XemmetJB

This Jetbrains plugin is currently somewhat experimental. What it does it allows you to turn an emmet expression into HTML, or XML. If your emmet expression starts at the beginning of the line (excluding typical white-space characters), then it will expand multiline, otherwise inline.

Currently you need to install [xemmet](https://github.com/peteraba/xemmet) on your system, it does not come bundled with the extension!

![Build](https://github.com/peteraba/xemmetjb/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/xemmetjb)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/xemmetjb)

## What's Xemmet?

Xemmet is more or less a dialect of Emmet, which is a markup language for expanding CSS rules into HTML. ([docs](https://www.emmet.io/), [cheatsheet](https://devhints.io/emmet))

How is Xemmet different from Emmet?

Perhaps most importantly, Xemmet does not - and likely will not - provide a lot of browser behaviour features that Emmet promises to provide, such as selection manipulation features, etc. Nor is it planned to provide features for writing .css files.

While it may change, Xemmet currently does not provide the [implicit tag names](https://docs.emmet.io/abbreviations/implicit-names/) feature.

Finally, it targets multiple formats. XML and HTML should be quite obvious, but there's also a specific one for using [HTMX](https://htmx.org/) and [Templ](https://github.com/a-h/templ) together. See the HTMX section below for details.

### Formats

- Ctrl+Alt+H - Translate Xemmet expression to HTML.
- Ctrl+Alt+X - Translate Xemmet expression to XML.
- Ctrl+Alt+0 - Translate Xemmet expression to [HTMX](https://htmx.org/) for [Templ](https://github.com/a-h/templ).

While the HTML and XML generation is likely straight-forward.

### Behaviors

Xemmet and XemmetJB have two primary modes: inline and multiline. XemmetJB will try to guess which one to used based on the position of the Xemmet expression. XemmetJB will try to figure out if the template is itself multiline, or if the xemmet expression is the first thing (other than spaces and tabs) on the line. If either is true, the response will be multiline, otherwise inline.

#### Example #1

The following would be considered inline, if xemmet was triggered with cursor being at the end of the line:

```
# This is an example a
```

If triggered with HTML target, it would turn into the following:

```
# This is an example <a href="#"></a>
```

Given an XML trigger, it would turn into the following:

```
# This is an example <a/>
```

Why? because if is not a recognized XML tag with standard properties.

If the target would HTMX, the result would look like this:

```
# This is an example <a/>
```

#### Example #2

The following would be considered multiline, if xemmet was triggered with cursor being at the end of the line:

```
  p>br
```

If triggered with HTML target, it would turn into the following:

```
  <p>
    <br>
  </p>
```

Given an XML trigger, it would turn into the following:

```
  <p>
    <br />
  </p>
```

If the target would HTMX, the result would be the same as for XML, because templ requires XML-like short tags to work properly.

## Template ToDo list

- [x] Create a new [IntelliJ Platform Plugin Template][template] project.
- [ ] Get familiar with the [template documentation][template].
- [x] Adjust the [pluginGroup](./gradle.properties), [plugin ID](./src/main/resources/META-INF/plugin.xml) and [sources package](./src/main/java).
- [ ] Adjust the plugin description in `README` (see [Tips][docs:plugin-description])
- [ ] Review the [Legal Agreements](https://plugins.jetbrains.com/docs/marketplace/legal-agreements.html?from=IJPluginTemplate).
- [ ] [Publish a plugin manually](https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html?from=IJPluginTemplate) for the first time.
- [ ] Set the `PLUGIN_ID` in the above README badges.
- [ ] Set the [Plugin Signing](https://plugins.jetbrains.com/docs/intellij/plugin-signing.html?from=IJPluginTemplate) related [secrets](https://github.com/JetBrains/intellij-platform-plugin-template#environment-variables).
- [ ] Set the [Deployment Token](https://plugins.jetbrains.com/docs/marketplace/plugin-upload.html?from=IJPluginTemplate).
- [ ] Click the <kbd>Watch</kbd> button on the top of the [IntelliJ Platform Plugin Template][template] to be notified about releases containing new features and fixes.

<!-- Plugin description -->
This Fancy IntelliJ Platform Plugin is going to be your implementation of the brilliant ideas that you have.

This specific section is a source for the [plugin.xml](/src/main/resources/META-INF/plugin.xml) file which will be extracted by the [Gradle](/build.gradle.kts) during the build process.

To keep everything working, do not remove `<!-- ... -->` sections. 
<!-- Plugin description end -->

## Installation


- Using the IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "xemmetjb"</kbd> >
  <kbd>Install</kbd>
  
- Manually:

  Download the [latest release](https://github.com/peteraba/xemmetjb/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
