[![Published on Vaadin  Directory](https://img.shields.io/badge/Vaadin%20Directory-published-00b4f0.svg)](https://https://vaadin.com/directory/component/beantable)
[![Stars on Vaadin Directory](https://img.shields.io/vaadin-directory/star/beantable.svg)](https://https://vaadin.com/directory/component/beantable)

# Bean Table

This is a simple html table based component backed by DataProvider. The data provider populates the Table with data from the beans. The component has minimal API and ultra simple design. The purpose of this component is to be a little sibling to Grid. Thus there are many features intentionally left out such as multiple header rows and header groupings.

This component does support basic support lazy loading of the data in paged mode. Non paged mode is purposed for the small data sets only. In other words this is designed for use cases where Grid is too heavy, overkill, etc.

- The latest version attempts to add top-notch accessibility support when focus behavior is enabled.

- There is proper keyboard navigation when focus behavior is enabled.

- BeanTable's cells can be populated by text, html or components. Also Tooltip provider can be applied.

- There are theme variants for striped rows, padding, wrapping and border styles.

- The component has css class name "bean-table" and custom css can be applied with it. CSS class name generator can be applied also.

- There is rudimentary paging support for larger sets of data. This seems to work well also with accessibility.

- There is selection mode for multiple rows.

- There is opt in integrated menu for column hide/show and theme variant to show row indexes.

The component seems to have better performance than Grid when using Firefox as a browser. When using Chrome, the case is the opposite by Grid rendering faster.

This component is demonstration of how powerful Vaadin's Element API is. The implementation has been done almost fully with Java, except small pieces of JavaScript needed for keyboard navigation and focus handling. Thus some 90% of the functionality can be asserted by regular unit tests, that can be found in

`src/test/java/org/vaadin/tatu/BeanTableTest.java`

The integration tests are in. 

`src/test/java/org/vaadin/tatu/BeanTableIT.java`

These mainly cover some user interaction that cannot be asserted in unit tests such as keyboard navigation and mouse actions.

## Demo

[Live demo](https://vaadin.com/directory/component/beantable)

## Release notes

### 2.10.0
- Fix keyboard navigation getting lost with PreserveOnRefresh and improve keyboard navigation
- Small performance improvement in keyboard events
- Improve A11y of the row selection
- Added item click event

### 2.9.1

- Fix column menu button disappearing when last column is hidden
- Fix column menu button styles
- Fix issue when using callback dataprovider without
- Small performance improvement in programmatic column visibility toggling

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
