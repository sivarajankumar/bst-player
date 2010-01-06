/*
 * Copyright 2009 Sikirulai Braheem
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.bramosystems.oss.player.showcase.client;

import com.bramosystems.oss.player.resources.sources.Links;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.resources.client.ExternalTextResource;
import com.google.gwt.resources.client.ResourceCallback;
import com.google.gwt.resources.client.ResourceException;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.user.client.ui.*;

/**
 *
 * @author Sikirulai Braheem <sbraheem at gmail.com>
 */
public abstract class AbstractCase extends Composite {

    private VerticalPanel casePanel;
    private Label caseHeader;

    public AbstractCase() {
        DockLayoutPanel dc = new DockLayoutPanel(Unit.PX);
        initWidget(dc);

        caseHeader = new Label();
        caseHeader.setStyleName("case-header");
        dc.addNorth(caseHeader, 40);

        ScrollPanel sp = new ScrollPanel();
        sp.setStyleName("case-content");
        dc.add(sp);

        casePanel = new VerticalPanel();
        casePanel.setWidth("80%");
        casePanel.setSpacing(10);
    }

    protected final void addCase(String title, String description, Widget player,
            ExternalTextResource codeSrc) {
        if (title != null) {
            Label tlbl = new Label(title);
            tlbl.setStyleName("section-desc");
            casePanel.add(tlbl);
        }

        if (description != null) {
            Label dlbl = new Label(description);
            dlbl.setStyleName("media-desc");
            casePanel.add(dlbl);
        }

        if (player != null) {
            casePanel.add(player);
        }

        if (codeSrc != null) {
            final HTML src = new HTML();
            try {
                codeSrc.getText(new ResourceCallback<TextResource>() {

                    public void onError(ResourceException e) {
                        src.setHTML("<div>Failed to load code sample!</div>");
                    }

                    public void onSuccess(TextResource resource) {
                        src.setHTML(resource.getText());
                    }
                });
            } catch (ResourceException ex) {
                src.setHTML("<div>Failed to load code sample!</div>");
            }
            casePanel.add(src);
        }
    }

    public final void clearCases() {
        casePanel.clear();
    }

    public void initCase(Links link) {
        caseHeader.setText(link.getTitle());
    }
}
