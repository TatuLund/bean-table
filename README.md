# Bean Table

This is a simple Table component backed by DataProvider. The data provider populates the Table with data from the beans. The component has minimal API and ultra simple design. The purpose of this component is to be a little sibling to Grid. Thus there are many features intentionally left out. This component does not support lazy loading of the data, thus it is purposed for the small data sets only. In other words this is designed for use cases where Grid is too heavy, overkill, etc.

BeanTable's cells can be populated by text, html or components.

Currently only minimal styling included, no scrolling, etc. provided.

The component has css class name "bean-table" and custom css can be applied with it.

## Release notes

### 1.3.0
- Backport features from 2.8.0
- Added menu for column visibility selection
- Added API to control column visibility
- Added row index variant
- Added theme variants for stripes, bordering, padding and wrapping
- Added cell focus behaviors, which are useful with A11y. By default no focus behavior.
- Add A11y features for screenreader friendliness
- Add getPage / setPage
- Fixed issue page not being adjusted when filtering
- Added Tooltip support for table cells and paging buttons
- Added Column width API
- Added Column alignment API
- Added getters for the Column and List
- Added setHeader(Component) overloading to set component instead of text in header. Hint: use Html component for adding html content.

### 1.2.0
- Added BeanTable#setClassNameProvider and Column#setClassNameProvider
- Added support for Column setter chaining

### 1.1.1
- Fix issue with setItems(..) not clearing old items

### 1.1.0
- Add paging support to make BeanTable usable for lazy data providers

### 1.0.0 
- First release

## Development instructions

JavaScript modules can either be published as an NPM package or be kept as local 
files in your project. The local JavaScript modules should be put in 
`src/main/resources/META-INF/frontend` so that they are automatically found and 
used in the using application.

If the modules are published then the package should be noted in the component 
using the `@NpmPackage` annotation in addition to using `@JsModule` annotation.


Starting the test/demo server:
1. Run `mvn jetty:run`.
2. Open http://localhost:8080 in the browser.

## Publishing to Vaadin Directory

You can create the zip package needed for [Vaadin Directory](https://vaadin.com/directory/) using
```
mvn versions:set -DnewVersion=1.0.0 # You cannot publish snapshot versions 
mvn install -Pdirectory
```

The package is created as `target/beantable-1.0.0.zip`

For more information or to upload the package, visit https://vaadin.com/directory/my-components?uploadNewComponent
