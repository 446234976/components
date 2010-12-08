package org.richfaces.component;

import javax.faces.component.UIComponentBase;

import org.richfaces.cdk.annotations.Attribute;
import org.richfaces.cdk.annotations.EventName;
import org.richfaces.cdk.annotations.JsfComponent;
import org.richfaces.cdk.annotations.JsfRenderer;
import org.richfaces.cdk.annotations.Tag;
import org.richfaces.renderkit.html.DropDownMenuRendererBase;

@JsfComponent(family = AbstractDropDownMenu.COMPONENT_FAMILY, type = AbstractDropDownMenu.COMPONENT_TYPE, 
        renderer=@JsfRenderer(type = DropDownMenuRendererBase.RENDERER_TYPE), 
        tag = @Tag(name="dropDownMenu"),
        attributes = {"events-props.xml", "core-props.xml", "i18n-props.xml"})
public abstract class AbstractDropDownMenu extends UIComponentBase 
        implements MenuComponent {

    public static final String COMPONENT_TYPE = "org.richfaces.DropDownMenu";

    public static final String COMPONENT_FAMILY = "org.richfaces.DropDownMenu";

    @Attribute
    public abstract String getShowEvent();

    @Attribute(defaultValue = MenuComponent.MODE_SERVER)
    public abstract String getMode();
    
    @Attribute
    public abstract boolean isDisabled();
    
    @Attribute(defaultValue = "300")
    public abstract int getHideDelay();
    
    @Attribute(defaultValue = "50")
    public abstract int getShowDelay();
    
    @Attribute(defaultValue = "250")
    public abstract int getPopupWith();
    
    @Attribute(defaultValue = "auto")
    public abstract String getJointPoint();
    
    @Attribute(defaultValue = "auto")
    public abstract String getDirection();    
    
    @Attribute(events = @EventName("groupshow"))
    public abstract String getOngroupshow();
    
    @Attribute(events = @EventName("grouphide"))
    public abstract String getOngrouphide();
    
    @Attribute(events = @EventName("show"))
    public abstract String getOnshow();
    
    @Attribute(events = @EventName("hide"))
    public abstract String getOnhide();
    
    @Attribute(events = @EventName("itemclick"))
    public abstract String getOnitemclick();    
    
    public enum Facets {
        LABEL("label"), LABEL_DISABLED("labelDisabled");
        
        private String facetName;
        private Facets(String name) {
            this.facetName = name;
        }
        
        @Override
        public String toString() {
            return facetName;
        }
    }
}
