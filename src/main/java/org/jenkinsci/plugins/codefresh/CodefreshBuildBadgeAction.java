/*
 * The MIT License
 *
 * Copyright 2017 antweiss.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.plugins.codefresh;

import hudson.model.BuildBadgeAction;

/**
 *
 * @author antweiss
 */
public class CodefreshBuildBadgeAction implements BuildBadgeAction {

    private String buildUrl;
    private final String buildStatus;
    private final String iconFile;
    private final String type;
    private String displayName;

    public CodefreshBuildBadgeAction(String buildUrl, String buildStatus, String type) {
        super();
        this.buildUrl = buildUrl;
        this.buildStatus = buildStatus;
        this.type = type;
        this.displayName = "Codefresh " + type + " Url";
        switch (buildStatus) {
            case "success":
                this.iconFile = "/plugin/codefresh/images/16x16/leaves_green.png";
                break;
            case "unstable":
                this.iconFile = "/plugin/codefresh/images/16x16/leaves_yellow.png";
                break;
            case "error":
                this.iconFile = "/plugin/codefresh/images/16x16/leaves_red.png";
                break;
            default:
                this.iconFile = "/plugin/codefresh/images/16x16/leaves_red.png";
        }
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getIconFileName() {
        return iconFile;
    }

    @Override
    public String getUrlName() {
        return buildUrl;
    }

    public String getType() {
        return type;
    }

    public void setUrl(String Url) {
        this.buildUrl = Url;
    }

    public void setDisplayName(String name) {
        this.displayName = name;
    }
}
