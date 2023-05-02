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

This component is demonstration of how powerful Vaadin's Element API is. The implementation has been done almost fully with Java, except small pieces of JavaScript needed for keyboard navigation and focus handling. Thus some 90% of the functionality can be asserted by regular unit tests, that can be found in.

`src/test/java/org/vaadin/tatu/BeanTableTest.java`

The integration tests are in. These mainly cover some user interaction that cannot be asserted in unit tests such as keyboard navigation and mouse actions.

`src/test/java/org/vaadin/tatu/BeanTableIT.java`

## Demo
[Live demo](https://vaadin.com/directory/component/beantable)

## Release notes

### 3.1.3

- Added an alert text for "No data" when the table is empty
- Added an error alert text "Fetching data failed" when data provider fetch fails

### 3.1.2

- Fix keyboard navigation getting lost with PreserveOnRefresh

### 3.1.1

- Small performance improvement in keyboard events

### 3.1.0

- Improve keyboard navigation
- Improve A11y of the row selection
- Added item click event

### 3.0.3

- Fix column menu button disappearing when last column is hidden
- Fix issue when using callback dataprovider without

### 3.0.2

- Fix column menu button styles

### 3.0.1

- Small performance improvement in programmatic column visibility toggling

### 3.0.0
- First version to support Vaadin 24

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
