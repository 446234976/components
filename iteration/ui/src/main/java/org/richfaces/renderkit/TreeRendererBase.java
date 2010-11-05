/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.richfaces.renderkit;

import static org.richfaces.component.AbstractTree.SELECTION_META_COMPONENT_ID;
import static org.richfaces.renderkit.util.AjaxRendererUtils.AJAX_FUNCTION_NAME;
import static org.richfaces.renderkit.util.AjaxRendererUtils.buildAjaxFunction;
import static org.richfaces.renderkit.util.AjaxRendererUtils.buildEventOptions;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import javax.faces.component.ContextCallback;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.PartialResponseWriter;
import javax.faces.context.PartialViewContext;
import javax.faces.context.ResponseWriter;

import org.ajax4jsf.javascript.JSFunction;
import org.ajax4jsf.javascript.JSReference;
import org.richfaces.component.AbstractTree;
import org.richfaces.component.AbstractTreeNode;
import org.richfaces.component.MetaComponentResolver;
import org.richfaces.component.SwitchType;
import org.richfaces.event.TreeSelectionEvent;
import org.richfaces.log.Logger;
import org.richfaces.log.RichfacesLogger;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;


/**
 * @author Nick Belaevski
 * 
 */
public abstract class TreeRendererBase extends RendererBase implements MetaComponentRenderer {

    static final Logger LOGGER = RichfacesLogger.RENDERKIT.getLogger();

    private static final JSReference PARAMS_JS_REF = new JSReference("params");

    private static final JSReference SOURCE_JS_REF = new JSReference("source");

    private static final String SELECTION_STATE = "__SELECTION_STATE";

    /**
     * @author Nick Belaevski
     * 
     */
    private final class RowKeyContextCallback implements ContextCallback {
        private Object rowKey;

        public void invokeContextCallback(FacesContext context, UIComponent target) {
            AbstractTreeNode treeNode = (AbstractTreeNode) target;
            rowKey = treeNode.findTreeComponent().getRowKey();
        }

        public Object getRowKey() {
            return rowKey;
        }
    }

    enum NodeState {
        expanded("rf-tr-nd-exp", "rf-trn-hnd-exp", "rf-trn-ico-nd"), 
        collapsed("rf-tr-nd-colps", "rf-trn-hnd-colps", "rf-trn-ico-nd"), 
        leaf("rf-tr-nd-lf", "rf-trn-hnd-lf", "rf-trn-ico-lf");

        private String nodeClass;

        private String handleClass;

        private String iconClass;

        private NodeState(String nodeClass, String handleClass, String iconClass) {
            this.nodeClass = nodeClass;
            this.handleClass = handleClass;
            this.iconClass = iconClass;
        }

        public String getNodeClass() {
            return nodeClass;
        }

        public String getHandleClass() {
            return handleClass;
        }

        public String getIconClass() {
            return iconClass;
        }

    }

    static final class QueuedData {

        private Object rowKey;

        private boolean lastNode;

        private boolean expanded;

        private boolean encoded;

        public QueuedData(Object rowKey, boolean lastNode, boolean expanded) {
            this.rowKey = rowKey;
            this.lastNode = lastNode;
            this.expanded = expanded;
        }

        public void setEncoded(boolean encoded) {
            this.encoded = encoded;
        }

        public boolean isEncoded() {
            return encoded;
        }

        public Object getRowKey() {
            return rowKey;
        }

        public boolean isLastNode() {
            return lastNode;
        }

        public boolean isExpanded() {
            return expanded;
        }
    }

    public void encodeTree(FacesContext context, UIComponent component) throws IOException {
        AbstractTree tree = (AbstractTree) component;

        new TreeEncoderFull(context, tree).encode();
    }

    protected String getAjaxSubmitFunction(FacesContext context, UIComponent component) {
        AbstractTree tree = (AbstractTree) component;

        if (tree.getToggleType() != SwitchType.ajax && tree.getSelectionType() != SwitchType.ajax) {
            return null;
        }

        JSFunction ajaxFunction = buildAjaxFunction(context, component, AJAX_FUNCTION_NAME);
        AjaxEventOptions eventOptions = buildEventOptions(context, component);

        eventOptions.setAjaxComponent(SOURCE_JS_REF);
        eventOptions.setClientParameters(PARAMS_JS_REF);

        if (!eventOptions.isEmpty()) {
            ajaxFunction.addParameter(eventOptions);
        }
        return ajaxFunction.toScript();
    }

    protected void encodeSelectionStateInput(FacesContext context, UIComponent component) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        writer.startElement(HtmlConstants.INPUT_ELEM, component);
        writer.writeAttribute(HtmlConstants.TYPE_ATTR, "hidden", null);
        String selectionStateInputId = getSelectionStateInputId(context, component);
        writer.writeAttribute(HtmlConstants.NAME_ATTRIBUTE, selectionStateInputId, null);
        writer.writeAttribute(HtmlConstants.ID_ATTRIBUTE, selectionStateInputId, null);
        writer.writeAttribute(HtmlConstants.CLASS_ATTRIBUTE, "rf-tr-sel-inp", null);

        String selectedNodeId = "";
        AbstractTree tree = (AbstractTree) component;

        Iterator<Object> selectedKeys = tree.getSelection().iterator();

        if (selectedKeys.hasNext()) {
            Object selectionKey = selectedKeys.next();
            Object initialKey = tree.getRowKey();
            try {
                tree.setRowKey(context, selectionKey);
                if (tree.isRowAvailable()) {
                    selectedNodeId = tree.findTreeNodeComponent().getClientId(context);
                }
            } finally {
                try {
                    tree.setRowKey(context, initialKey);
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }

        if (selectedKeys.hasNext()) {
            //TODO - better message
            throw new IllegalArgumentException("Selection object should not contain more than selected keys!");
        }

        writer.writeAttribute(HtmlConstants.VALUE_ATTRIBUTE, selectedNodeId, null);

        writer.endElement(HtmlConstants.INPUT_ELEM);
    }

    protected String getSelectionStateInputId(FacesContext context, UIComponent component) {
        return component.getClientId(context) + SELECTION_STATE;
    }

    protected SwitchType getSelectionMode(FacesContext context, UIComponent component) {
        AbstractTree tree = (AbstractTree) component;

        SwitchType selectionType = tree.getSelectionType();
        if (selectionType != null && selectionType != SwitchType.ajax && selectionType != SwitchType.client) {
            //TODO - better message
            throw new IllegalArgumentException(String.valueOf(selectionType));
        }

        return selectionType;
    }

    public void encodeMetaComponent(FacesContext context, UIComponent component, String metaComponentId)
        throws IOException {

        if (SELECTION_META_COMPONENT_ID.equals(metaComponentId)) {
            PartialResponseWriter writer = context.getPartialViewContext().getPartialResponseWriter();
            
            writer.startUpdate(getSelectionStateInputId(context, component));
            encodeSelectionStateInput(context, component);
            writer.endUpdate();
            
            writer.startEval();

            JSFunction function = new JSFunction("RichFaces.$", component.getClientId(context));
            writer.write(function.toScript() + ".__updateSelection();");
            
            writer.endEval();
        } else {
            throw new IllegalArgumentException(metaComponentId);
        }

        // TODO Auto-generated method stub

    }

    public void decodeMetaComponent(FacesContext context, UIComponent component, String metaComponentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void decode(FacesContext context, UIComponent component) {
        super.decode(context, component);

        Map<String, String> map = context.getExternalContext().getRequestParameterMap();
        String selectedNode = map.get(getSelectionStateInputId(context, component));
        AbstractTree tree = (AbstractTree) component;

        Object selectionRowKey = null;

        if (!Strings.isNullOrEmpty(selectedNode)) {
            RowKeyContextCallback rowKeyContextCallback = new RowKeyContextCallback();
            tree.invokeOnComponent(context, selectedNode, rowKeyContextCallback);
            selectionRowKey = rowKeyContextCallback.getRowKey();
        }

        Collection<Object> selection = tree.getSelection();

        Collection<Object> newSelection = null;
        
        if (selectionRowKey == null) {
            if (!selection.isEmpty()) {
                newSelection = Collections.emptySet();
            }
        } else {
            if (!selection.contains(selectionRowKey)) {
                newSelection = Collections.singleton(selectionRowKey);
            }
        }
        
        if (newSelection != null) {
            new TreeSelectionEvent(component, Sets.newHashSet(selection), newSelection).queue();
        }

        PartialViewContext pvc = context.getPartialViewContext();
        if (pvc.isAjaxRequest()) {
            pvc.getRenderIds().add(tree.getClientId(context) + MetaComponentResolver.META_COMPONENT_SEPARATOR_CHAR
                + AbstractTree.SELECTION_META_COMPONENT_ID);
        }
    }
}
