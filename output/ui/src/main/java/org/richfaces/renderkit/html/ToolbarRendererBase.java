/*
 * JBoss, Home of Professional Open Source
 * Copyright ${year}, Red Hat, Inc. and individual contributors
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

package org.richfaces.renderkit.html;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.faces.application.ResourceDependency;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.richfaces.component.AbstractToolbar;
import org.richfaces.component.AbstractToolbarGroup;
import org.richfaces.renderkit.ComponentAttribute;
import org.richfaces.renderkit.HtmlConstants;
import org.richfaces.renderkit.RenderKitUtils;
import org.richfaces.renderkit.RendererBase;


@ResourceDependency(library = "org.richfaces", name = "toolbar.ecss")
public abstract class ToolbarRendererBase extends RendererBase {

    public static final String RENDERER_TYPE = "org.richfaces.ToolbarRenderer";
    
    public static final Map<String, ComponentAttribute> ITEMS_HANDLER_ATTRIBUTES = Collections
    .unmodifiableMap(ComponentAttribute.createMap(
            new ComponentAttribute(HtmlConstants.ONCLICK_ATTRIBUTE)
                .setEventNames("itemclick")
                .setComponentAttributeName("onitemclick"),
            new ComponentAttribute(HtmlConstants.ONDBLCLICK_ATTRIBUTE)
                .setEventNames("itemdblclick")
                .setComponentAttributeName("onitemdblclick"), 
            new ComponentAttribute(HtmlConstants.ONMOUSEDOWN_ATTRIBUTE)
                .setEventNames("itemmousedown")
                .setComponentAttributeName("onitemmousedown"),
            new ComponentAttribute(HtmlConstants.ONMOUSEUP_ATTRIBUTE)
                .setEventNames("itemmouseup")
                .setComponentAttributeName("onitemmouseup"), 
            new ComponentAttribute(HtmlConstants.ONMOUSEOVER_ATTRIBUTE)
                .setEventNames("itemmouseover")
                .setComponentAttributeName("onitemmouseover"), 
            new ComponentAttribute(HtmlConstants.ONMOUSEMOVE_ATTRIBUTE)
                .setEventNames("itemmousemove")
                .setComponentAttributeName("onitemmousemove"), 
            new ComponentAttribute(HtmlConstants.ONMOUSEOUT_ATTRIBUTE)
                .setEventNames("itemmouseout")
                .setComponentAttributeName("onitemmouseout"), 
            new ComponentAttribute(HtmlConstants.ONKEYPRESS_ATTRIBUTE)
                .setEventNames("itemkeypress")
                .setComponentAttributeName("onitemkeypress"),
            new ComponentAttribute(HtmlConstants.ONKEYDOWN_ATTRIBUTE)
                .setEventNames("itemkeydown")
                .setComponentAttributeName("onitemkeydown"),
            new ComponentAttribute(HtmlConstants.ONKEYUP_ATTRIBUTE)
                .setEventNames("itemkeyup")
                .setComponentAttributeName("onitemkeyup")
    ));

    public enum ItemSeparators {
        NONE, SQUARE, DISC, GRID, LINE
    }

    public enum Locations {
        RIGHT, LEFT
    }

    @Override
    public void encodeChildren(FacesContext context, UIComponent component) throws IOException {
        AbstractToolbar toolbar = (AbstractToolbar) component;
        String itemClass = (String) toolbar.getAttributes().get("itemClass");
        String itemStyle = (String) toolbar.getAttributes().get("itemStyle");

        List<UIComponent> children = toolbar.getChildren();

        if (children != null) {
            List<UIComponent> childrenToTheLeft = new LinkedList<UIComponent>();
            List<UIComponent> childrenToTheRight = new LinkedList<UIComponent>();
            for (UIComponent child : children) {
                if (child.isRendered()) {
                    if (child instanceof AbstractToolbarGroup) {
                        AbstractToolbarGroup group = (AbstractToolbarGroup) child;
                        String location = group.getLocation();
                        if (location != null && location.equalsIgnoreCase(Locations.RIGHT.toString())) {
                            childrenToTheRight.add(child);
                        } else {
                            childrenToTheLeft.add(child);
                        }
                    } else {
                        childrenToTheLeft.add(child);
                    }
                }
            }

            ResponseWriter writer = context.getResponseWriter();
            for (Iterator<UIComponent> it = childrenToTheLeft.iterator(); it.hasNext();) {
                
                UIComponent child = it.next();
                
                if (!(child instanceof AbstractToolbarGroup)) {
                    writer.startElement(HtmlConstants.TD_ELEM, component);
                    writer.writeAttribute(HtmlConstants.CLASS_ATTRIBUTE, concatClasses("rf-tb-itm", itemClass), null);
                    if (isPropertyRendered(itemStyle)) {
                        writer.writeAttribute(HtmlConstants.STYLE_ATTRIBUTE, itemStyle, null);
                    }
                    encodeEventsAttributes(context, toolbar);
                }
                
                child.encodeAll(context);
                
                if (!(child instanceof AbstractToolbarGroup)) {
                    writer.endElement(HtmlConstants.TD_ELEM);
                }
                
                
                
                if (it.hasNext()) {
                    insertSeparatorIfNeed(context, toolbar, writer);
                }
            }

            writer.startElement(HtmlConstants.TD_ELEM, component);
            writer.writeAttribute(HtmlConstants.STYLE_ATTRIBUTE, "width:100%", null);
            writer.endElement(HtmlConstants.TD_ELEM);

            for (Iterator<UIComponent> it = childrenToTheRight.iterator(); it.hasNext();) {
                UIComponent child = it.next();
                child.encodeAll(context);
                if (it.hasNext()) {
                    insertSeparatorIfNeed(context, toolbar, writer);
                }
            }
        }
    }

    /**
     * Inserts separator between toolbar items. Uses facet "itemSeparator" if it
     * is set and default separator implementation if facet is not set.
     * 
     * @param context
     *            - faces context
     * @param component
     *            - component
     * @param writer
     *            - response writer
     * @throws IOException
     *             - in case of IOException during writing to the ResponseWriter
     */
    protected void insertSeparatorIfNeed(FacesContext context, UIComponent component, ResponseWriter writer)
        throws IOException {
        UIComponent separatorFacet = component.getFacet("itemSeparator");
        boolean isSeparatorFacetRendered = (separatorFacet != null) ? separatorFacet.isRendered() : false;
        if (isSeparatorFacetRendered) {
            writer.startElement(HtmlConstants.TD_ELEM, component);
            writer.writeAttribute(HtmlConstants.CLASS_ATTRIBUTE, "rf-tb-sep", null);
            separatorFacet.encodeAll(context);
            writer.endElement(HtmlConstants.TD_ELEM);
        } else {
            insertDefaultSeparatorIfNeed(context, component, writer);
        }
    }
    
    /**
     * Inserts default separator. Possible values are: "square", "disc", "grid",
     * "line" - for separators provided by component implementation; "none" -
     * for no separators between toolbar items; URI string value - for custom
     * images specified by the page author.
     * 
     * @param context
     *            - faces context
     * @param component
     *            - component
     * @param writer
     *            - response writer
     * @throws IOException
     *             - in case of IOException during writing to the ResponseWriter
     */
    protected void insertDefaultSeparatorIfNeed(FacesContext context, UIComponent component, ResponseWriter writer)
        throws IOException {
        String itemSeparator = (String) component.getAttributes().get("itemSeparator");
       
        if (itemSeparator != null && itemSeparator.trim().length() != 0
                && !itemSeparator.equalsIgnoreCase(ItemSeparators.NONE.toString())) {

            ItemSeparators separator = null;
            if (itemSeparator.equalsIgnoreCase(ItemSeparators.SQUARE.toString())) {
                separator = ItemSeparators.SQUARE;
            } else if (itemSeparator.equalsIgnoreCase(ItemSeparators.DISC.toString())) {
                separator = ItemSeparators.DISC;
            } else if (itemSeparator.equalsIgnoreCase(ItemSeparators.GRID.toString())) {
                separator = ItemSeparators.GRID;
            } else if (itemSeparator.equalsIgnoreCase(ItemSeparators.LINE.toString())) {
                separator = ItemSeparators.LINE;
            }

            writer.startElement(HtmlConstants.TD_ELEM, component);
            String separatorClass = "rf-tb-sep";
            separatorClass = concatClasses(separatorClass, (String) component.getAttributes().get("separatorClass"));
            writer.writeAttribute(HtmlConstants.CLASS_ATTRIBUTE, separatorClass, null);

            if (separator != null) {
                String itemSeparatorClass = "rf-tb-sep-" + separator.toString().toLowerCase();
                writer.startElement(HtmlConstants.DIV_ELEM, component);
                writer.writeAttribute(HtmlConstants.CLASS_ATTRIBUTE, itemSeparatorClass, null);
                writer.write("&nbsp;");
                writer.endElement(HtmlConstants.DIV_ELEM);
            } else {
                
                String uri = RenderKitUtils.getResourceURL(itemSeparator, context);
                writer.startElement(HtmlConstants.IMG_ELEMENT, component);
                writer.writeAttribute(HtmlConstants.SRC_ATTRIBUTE, uri, null);
                writer.writeAttribute(HtmlConstants.ALT_ATTRIBUTE, "", null);
                writer.endElement(HtmlConstants.IMG_ELEMENT);
            }
            
            writer.endElement(HtmlConstants.TD_ELEM);
        }
    }

    protected Class<? extends javax.faces.component.UIComponent> getComponentClass() {
        return AbstractToolbar.class;
    }

    public boolean getRendersChildren() {
        return true;
    }

    protected void encodeEventsAttributes(FacesContext facesContext, UIComponent component)
        throws IOException {
        RenderKitUtils.renderPassThroughAttributesOptimized(facesContext, component, ITEMS_HANDLER_ATTRIBUTES);
    }

    protected boolean isPropertyRendered(String property) {
        return (null != property && !"".equals(property));
    }
}
