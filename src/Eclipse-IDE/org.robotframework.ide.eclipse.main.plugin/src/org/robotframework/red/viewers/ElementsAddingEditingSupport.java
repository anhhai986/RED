/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.red.viewers;

import java.util.function.Supplier;

import org.eclipse.jface.viewers.AlwaysDeactivatingCellEditor;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import com.google.common.annotations.VisibleForTesting;

/**
 * @author Michal Anglart
 *
 */
public abstract class ElementsAddingEditingSupport extends EditingSupport {

    protected final int index;

    protected final Supplier<?> elementsCreator;

    public ElementsAddingEditingSupport(final ColumnViewer viewer, final int index,
            final Supplier<?> creator) {
        super(viewer);
        this.index = index;
        this.elementsCreator = creator;
    }

    @Override
    protected boolean canEdit(final Object element) {
        return true;
    }

    @Override
    protected CellEditor getCellEditor(final Object element) {
        if (element instanceof ElementAddingToken) {
            return new AlwaysDeactivatingCellEditor((Composite) getViewer().getControl());
        }
        return null;
    }

    @Override
    protected void setValue(final Object element, final Object value) {
        if (element instanceof ElementAddingToken) {
            scheduleViewerRefreshAndEditorActivation(elementsCreator.get());
        }
    }

    protected int getColumnShift() {
        return 0;
    }

    // refresh and cell editor activation has to be done in GUI thread but after
    // current cell editor was properly deactivated
    protected final void scheduleViewerRefreshAndEditorActivation(final Object value) {
        final Display display = getViewer().getControl().getDisplay();
        display.asyncExec(refreshAndEdit(value));
    }

    @VisibleForTesting
    Runnable refreshAndEdit(final Object value) {
        return () -> {
            final ColumnViewer viewer = getViewer();
            if (viewer.getControl() != null && viewer.getControl().isDisposed()) {
                return;
            }
            viewer.refresh();
            performPriorToEdit(value);
            if (value != null) {
                viewer.editElement(value, index + getColumnShift());
            }
        };
    }

    protected void performPriorToEdit(@SuppressWarnings("unused") final Object addedElement) {
        // nothing to do, override if needed
    }
}
