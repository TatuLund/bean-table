package org.vaadin.tatu;

public enum BeanTableVariant {
    NO_BORDER("no-border"),
    NO_ROW_BORDERS("no-row-borders"),
    COLUMN_BORDERS("column-borders"),
    ROW_STRIPES("row-stripes"),
    PADDING("padding"),
    WRAP_CELL_CONTENT("wrap-cell-content");

    private final String variant;

    BeanTableVariant(String variant) {
        this.variant = variant;
    }

    /**
     * Gets the variant name.
     *
     * @return variant name
     */
    public String getVariantName() {
        return variant;
    }
}
