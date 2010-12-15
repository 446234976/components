/*
 * code review by Pavel Yaschenko
 * 
 * No event's unbindings when component would be destroyed 
 * Hint: easy way to unbind - use namespaces when bind event handlers
 * 
 */

(function ($, rf) {
	
	rf.ui = rf.ui || {};
      
	rf.ui.Draggable =  function(id, options) {
		this.dragElement = $(document.getElementById(id));
		this.dragElement.draggable();

		if(options.indicator) {
			var element = document.getElementById(options.indicator);
			this.dragElement.data("indicator", true);
			this.dragElement.draggable("option", "helper", function(){return element});
		} else {
			this.dragElement.data("indicator", false);
			this.dragElement.draggable("option", "helper", 'clone');
		}
		
		this.dragElement.draggable("option", "addClasses", false);
		
		this.options = options;
		
		this.dragElement.data('type', this.options.type);
		this.dragElement.data("init", true);
			
		this.dragElement.bind('dragstart', $.proxy(this.dragStart, this));
		this.dragElement.bind('drag', $.proxy(this.drag, this));
		this.dragElement.bind('dragstop', $.proxy(this.dragStop, this));
    };
	
	var defaultOptions = {
	};

	$.extend(rf.ui.Draggable.prototype, ( function () {
    		return {
				dragStart: function(e, ui) {
					var element = ui.helper[0];
					this.parentElement = element.parentNode;
					ui.helper.detach().appendTo("body").setPosition(e).show();
		
					if(this.__isCustomDragIndicator()) {
						// move cursor to the center of custom dragIndicator;
						var left = (ui.helper.width()/2);
						var top = (ui.helper.height()/2);
						this.dragElement.data('draggable').offset.click.left = left;
						this.dragElement.data('draggable').offset.click.top = top;	
					}
    			}, 
				
				drag: function(e, ui) {
					if(this.__isCustomDragIndicator()) {
						var indicator = rf.$(this.options.indicator);
						if(indicator) {
							ui.helper.addClass(indicator.draggingClass());
						}
					}
					this.__clearDraggableCss(ui.helper);
				},
				
				dragStop: function(e, ui){
					ui.helper.hide().detach().appendTo(this.parentElement);
					if(ui.helper[0] != this.dragElement[0]) { 
							//fix to prevent remove custom indicator from DOM tree. see jQuery draggable._clear method for details
						ui.helper[0] = this.dragElement[0];
					}
				}, 
				
				__isCustomDragIndicator: function() {
					return this.dragElement.data("indicator"); 
				},
				
				__clearDraggableCss: function(element) {
					if(element && element.removeClass) {
						//draggable 'addClasses: false' doesn't work so clear jQuery style 
						element.removeClass("ui-draggable-dragging");	
					}
				}
			}
    	})());
})(jQuery, window.RichFaces);
