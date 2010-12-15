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

package org.richfaces.renderkit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.faces.application.ResourceDependencies;
import javax.faces.application.ResourceDependency;
import javax.faces.component.ActionSource;
import javax.faces.component.ContextCallback;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.component.behavior.ClientBehavior;
import javax.faces.component.behavior.ClientBehaviorContext;
import javax.faces.component.behavior.ClientBehaviorHolder;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;
import javax.faces.render.FacesBehaviorRenderer;
import javax.faces.render.RenderKitFactory;

import org.richfaces.component.behavior.ClientDragBehavior;
import org.richfaces.component.behavior.ClientDropBehavior;
import org.richfaces.component.behavior.DropBehavior;
import org.richfaces.event.DropEvent;


/**
 * @author abelevich
 *
 */


@FacesBehaviorRenderer(rendererType = DropBehavior.BEHAVIOR_ID, renderKitId = RenderKitFactory.HTML_BASIC_RENDER_KIT)

@ResourceDependencies({
    @ResourceDependency(name = "jquery.js"),
    @ResourceDependency(name = "jquery.position.js"),
    @ResourceDependency(name = "richfaces.js"),
    @ResourceDependency(name = "richfaces.js"),
    @ResourceDependency(library = "org.richfaces", name = "jquery-ui-core.js"),
    @ResourceDependency(library = "org.richfaces", name = "jquery-dnd.js"),
    @ResourceDependency(library = "org.richfaces", name = "dnd-droppable.js"),
    @ResourceDependency(library = "org.richfaces", name = "dnd-manager.js")
})
public class DropBehaviorRendererBase extends DnDBehaviorRenderBase {
    
    @Override
    public void decode(FacesContext facesContext, UIComponent component, ClientBehavior behavior) {
        if (null == facesContext || null == component || behavior == null) {
            throw new NullPointerException();
        }

        Map<String, String> requestParamMap = facesContext.getExternalContext().getRequestParameterMap();
        String dragSource = (String) requestParamMap.get("dragSource");

        DragBehaviorContextCallBack dragBehaviorContextCallBack = new DragBehaviorContextCallBack();
        facesContext.getViewRoot().invokeOnComponent(facesContext, dragSource, dragBehaviorContextCallBack);

        if(behavior instanceof ClientDropBehavior) {
            ClientDropBehavior dropBehavior = (ClientDropBehavior)behavior;
            DropEvent dropEvent = new DropEvent(component, dropBehavior);
            dropEvent.setDropValue(dropBehavior.getDropValue());
            dropEvent.setDragComponent(dragBehaviorContextCallBack.getDragComponent());
            dropEvent.setDragBehavior(dragBehaviorContextCallBack.getDragBehavior());
            dropEvent.setDragValue(dragBehaviorContextCallBack.getDragValue());
            queueEvent(dropEvent);
        }
    }
    
    @Override
    protected String getScriptName() {
        return "RichFaces.ui.DnDManager.droppable";
    }
    
    public Map<String, Object> getOptions(ClientBehaviorContext behaviorContext, ClientBehavior behavior) {
        Map<String, Object> options = new HashMap<String, Object>();

        if(behavior instanceof ClientDropBehavior) {
            ClientDropBehavior dropBehavior = (ClientDropBehavior)behavior;
            options.put("acceptedTypes", dropBehavior.getAcceptedTypes());
        }
        return options;
    }
    
    private final class DragBehaviorContextCallBack implements ContextCallback {
        
        private Object dragValue;
        
        private ClientDragBehavior dragBehavior;
        
        private UIComponent dragComponent;
        
        public void invokeContextCallback(FacesContext context, UIComponent target) {
            ClientDragBehavior dragBehavior = getDragBehavior(target, "mouseover");
            this.dragValue = dragBehavior.getDragValue();
            this.dragBehavior = dragBehavior;
            this.dragComponent = target;
        }
        
        public Object getDragValue() {
            return dragValue;
        }

        public ClientDragBehavior getDragBehavior() {
            return dragBehavior;
        }

        public UIComponent getDragComponent() {
            return dragComponent;
        }
        
        private ClientDragBehavior getDragBehavior(UIComponent parent, String event) {
            if(parent instanceof ClientBehaviorHolder) {
                Map<String, List<ClientBehavior>> behaviorsMap = ((ClientBehaviorHolder)parent).getClientBehaviors();
                Set<Map.Entry<String, List<ClientBehavior>>> entries = behaviorsMap.entrySet();
                
                for(Entry<String, List<ClientBehavior>> entry: entries) {
                    if(event.equals(entry.getKey())){
                        List<ClientBehavior> behaviors = entry.getValue();
                        for(ClientBehavior behavior: behaviors) {
                            if(behavior instanceof ClientDragBehavior) {
                                return (ClientDragBehavior)behavior;
                            }
                        }
                    }
                }
                
            }
            return null;
        }
        
    }
    
    protected void queueEvent(DropEvent dropEvent){
        UIComponent component = dropEvent.getComponent();
        ClientDropBehavior dropBehavior = dropEvent.getDropBehavior();
        
        if(component != null && dropBehavior != null) {
            PhaseId phaseId = PhaseId.INVOKE_APPLICATION;

            if (isImmediate(component, dropBehavior)) {
                phaseId = PhaseId.APPLY_REQUEST_VALUES;
            } else if (isBypassUpdates(component, dropBehavior)) {
                phaseId = PhaseId.PROCESS_VALIDATIONS;
            }
    
            dropEvent.setPhaseId(phaseId);
            component.queueEvent(dropEvent);
        }
    }
    
    private boolean isImmediate(UIComponent component, ClientDropBehavior dropBehavior){
        boolean immediate = dropBehavior.isImmediate();
        if(!immediate) {
            if (component instanceof EditableValueHolder) {
                immediate = ((EditableValueHolder) component).isImmediate();
            } else if (component instanceof ActionSource) {
                immediate = ((ActionSource) component).isImmediate();
            }
        }
        return immediate;
    }
    
    private boolean isBypassUpdates(UIComponent component, ClientDropBehavior dropBehavior){
        boolean bypassUpdates = dropBehavior.isBypassUpdates();
        if (!bypassUpdates) {
            bypassUpdates = getUtils().isBooleanAttribute(component, "bypassUpdates");
        }
        return bypassUpdates;
    }

}
