[![Published on Vaadin  Directory](https://img.shields.io/badge/Vaadin%20Directory-published-00b4f0.svg)](https://https://vaadin.com/directory/component/beantable)
[![Stars on Vaadin Directory](https://img.shields.io/vaadin-directory/star/beantable.svg)](https://https://vaadin.com/directory/component/beantable)

# Bean Table

This is a simple Table component backed by DataProvider. The data provider populates the Table with data from the beans. The component has minimal API and ultra simple design. The purpose of this component is to be a little sibling to Grid. Thus there are many features intentionally left out. This component does not support lazy loading of the data, thus it is purposed for the small data sets only. In other words this is designed for use cases where Grid is too heavy, overkill, etc.

BeanTable's cells can be populated by text, html or components.

Currently only minimal styling included, no scrolling, etc. provided.

The component has css class name "bean-table" and custom css can be applied with it.

## Release notes

### 2.9.0
- Added API for selection, enable, select, deselect (by default disabled)
- Improved keyboard navigation with arrow keys, space for select

### 2.8.0

- Added menu for column visibility selection
- Added API to control column visibility
- Added row index variant

### 2.7.0

- Added theme variants for stripes, bordering, padding and wrapping

### 2.6.0
- Added cell focus behaviors, which are useful with A11y. By default no focus behavior.
- Did some small improvements in A11y behaviors by testing the component with NVDA

### 2.5.0
- Added getPage / setPage
- Fixed issue page not being adjusted when filtering

### 2.4.1
- Fix NPE in paged mode
 
### 2.4.0
- Add A11y support

### 2.3.0
- Added tooltip support
- Added width API for column
- Added column alignment API

### 2.2.0
- Added getters for Column and List<Column>

### 2.1.1
- Fix issue with missing initial data reset using in-memory data, hence data not shown

### 2.1.0
- Add support for components in headers, Column#setHeader(Component)

### 2.0.0 
- First release with support for DataView API introduced in Vaadin 17

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
