/**
 * The MIT License
 * <p>
 * Copyright (c) 2019- Nordic Institute for Interoperability Solutions (NIIS)
 * Copyright (c) 2018 Estonian Information System Authority (RIA),
 * Nordic Institute for Interoperability Solutions (NIIS), Population Register Centre (VRK)
 * Copyright (c) 2015-2017 Estonian Information System Authority (RIA), Population Register Centre (VRK)
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.niis.xroad.test.ui.glue.constants;

import org.openqa.selenium.By;

import static org.openqa.selenium.By.xpath;

public final class Constants {

    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm";

    public static final By BTN_DIALOG_SAVE = xpath("//button[@data-test=\"dialog-save-button\"]");
    public static final By BTN_DIALOG_CANCEL = xpath("//button[@data-test=\"dialog-cancel-button\"]");
    public static final By BTN_DIALOG_DELETE = xpath("//button[@data-test=\"dialog-delete-button\"]");

    public static final By BTN_CLOSE_X = xpath("//i[@data-test=\"close-x\"]");
    public static final By SNACKBAR_SUCCESS = xpath("//div[@data-test=\"success-snackbar\"]");
    public static final By BTN_CLOSE_SNACKBAR = By.xpath("//button[@data-test=\"close-snackbar\"]");
    public static final By BTN_INFO_CARD_EDIT = By.xpath("//button[@data-test=\"info-card-edit-button\"]");
    public static final By INPUT_FILE_UPLOAD = xpath("//input[@type=\"file\"]");

    private Constants() {
    }
}
