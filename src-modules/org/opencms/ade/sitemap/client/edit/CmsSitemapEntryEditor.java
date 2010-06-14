/*
 * File   : $Source: /alkacon/cvs/opencms/src-modules/org/opencms/ade/sitemap/client/edit/Attic/CmsSitemapEntryEditor.java,v $
 * Date   : $Date: 2010/06/14 08:08:41 $
 * Version: $Revision: 1.5 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Management System
 *
 * Copyright (C) 2002 - 2009 Alkacon Software (http://www.alkacon.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * For further information about Alkacon Software, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.ade.sitemap.client.edit;

import org.opencms.ade.sitemap.client.Messages;
import org.opencms.ade.sitemap.client.control.CmsSitemapController;
import org.opencms.ade.sitemap.client.ui.CmsTemplateSelectBox;
import org.opencms.ade.sitemap.client.ui.CmsTemplateSelectCell;
import org.opencms.ade.sitemap.shared.CmsClientSitemapEntry;
import org.opencms.ade.sitemap.shared.CmsSitemapTemplate;
import org.opencms.gwt.client.CmsCoreProvider;
import org.opencms.gwt.client.ui.input.CmsCheckBox;
import org.opencms.gwt.client.ui.input.CmsNonEmptyValidator;
import org.opencms.gwt.client.ui.input.CmsTextBox;
import org.opencms.gwt.client.ui.input.I_CmsFormField;
import org.opencms.gwt.client.ui.input.I_CmsValidationHandler;
import org.opencms.gwt.client.ui.input.form.CmsBasicFormField;
import org.opencms.gwt.client.ui.input.form.CmsForm;
import org.opencms.gwt.client.ui.input.form.CmsFormDialog;
import org.opencms.gwt.client.ui.input.form.I_CmsFormResetHandler;
import org.opencms.gwt.client.util.CmsPair;
import org.opencms.util.CmsStringUtil;
import org.opencms.xml.content.CmsXmlContentProperty;
import org.opencms.xml.sitemap.CmsSitemapManager;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * A dialog for editing the properties, title, url name and template of a sitemap entry.<p>
 * 
 *  @author Georg Westenberger
 *  
 *  @version $Revision: 1.5 $
 *  
 *  @since 8.0.0
 */
public class CmsSitemapEntryEditor extends CmsFormDialog {

    /**
     * The editor mode.<p>
     */
    public enum Mode {

        /** Collision resolution while drag'n drop. */
        DND,

        /** Edition of an existing entry. */
        EDIT,

        /** Creation of a new entry. */
        NEW;
    }

    /** The key for the default template. */
    private static final String DEFAULT_TEMPLATE_VALUE = "";

    /** The field id of the 'template' property. */
    private static final String FIELD_TEMPLATE = "template";

    /** The field id of the 'template-inherited' property. */
    private static final String FIELD_TEMPLATE_INHERIT_CHECKBOX = "field_template_inherited";

    /** The field id of the "title" form field. */
    private static final String FIELD_TITLE = "field_title";

    /** The field id of the "url name" form field. */
    private static final String FIELD_URLNAME = "field_urlname";

    /** The sitemap controller which changes the actual entry data when the user clicks OK in this dialog. */
    protected CmsSitemapController m_controller;

    /** The handler for this sitemap entry editor. */
    protected I_CmsSitemapEntryEditorHandler m_handler;

    /** The configuration of the properties. */
    private Map<String, CmsXmlContentProperty> m_propertyConfig;

    /**
     * Creates a new sitemap entry editor.<p>
     * 
     * @param handler the handler
     */
    public CmsSitemapEntryEditor(I_CmsSitemapEntryEditorHandler handler) {

        super(Messages.get().key(Messages.GUI_PROPERTY_EDITOR_TITLE_0));

        m_controller = handler.getController();
        m_propertyConfig = removeHiddenProperties(m_controller.getData().getProperties());
        m_handler = handler;
    }

    /**
     * Shows the sitemap entry editor to the user.<p>
     */
    public void start() {

        CmsForm form = getForm();

        form.addLabel(m_handler.getDescriptionText());

        CmsClientSitemapEntry entry = m_handler.getEntry();
        if (!entry.isRoot()) {
            // the root entry name can't be edited 
            CmsBasicFormField urlNameField = createUrlNameField(entry);
            form.addField(urlNameField);
        }
        form.addResetHandler(new I_CmsFormResetHandler() {

            /**
             * @see org.opencms.gwt.client.ui.input.form.I_CmsFormResetHandler#onResetForm()
             */
            public void onResetForm() {

                showUrlNameError(null);
            }
        });

        CmsBasicFormField titleField = createTitleField(entry);
        form.addField(titleField);

        Map<String, String> properties = entry.getProperties();
        String propTemplate = properties.get(CmsSitemapManager.Property.template.toString());
        String propTemplateInherited = properties.get(CmsSitemapManager.Property.templateInherited.toString());
        boolean inheritTemplate = (propTemplate != null) && propTemplate.equals(propTemplateInherited);
        CmsBasicFormField templateField = createTemplateField();
        String initialTemplate = propTemplate != null ? propTemplate : "";
        form.addField(templateField, initialTemplate);

        CmsBasicFormField templateInheritField = createTemplateInheritField();
        form.addField(templateInheritField, "" + inheritTemplate);

        form.addSeparator();
        Map<String, I_CmsFormField> formFields = CmsBasicFormField.createFields(m_propertyConfig.values());
        for (I_CmsFormField field : formFields.values()) {
            String currentValue = properties.get(field.getId());
            form.addField(field, currentValue);
        }
        center();
    }

    /**
     * Helper method which retrieves a value for a given key from a map and then deletes the entry for the key.<p>
     * 
     * @param map the map from which to retrieve the value 
     * @param key the key
     * 
     * @return the removed value  
     */
    protected String getAndRemoveValue(Map<String, String> map, String key) {

        String value = map.get(key);
        if (value != null) {
            map.remove(key);
        }
        return value;
    }

    /** 
     * Handles a form submit after the normal fields have already been validated successfully.<p>
     */
    protected void handleSubmit() {

        final Map<String, String> fieldValues = getForm().collectValues();
        final String titleValue = getAndRemoveValue(fieldValues, FIELD_TITLE);

        CmsPair<String, String> templateProps = getTemplateProperties(fieldValues);
        fieldValues.put(CmsSitemapManager.Property.template.toString(), templateProps.getFirst());
        fieldValues.put(CmsSitemapManager.Property.templateInherited.toString(), templateProps.getSecond());
        if (m_handler.getEntry().isRoot()) {
            // The root element's name can't be edited 
            hide();
            m_handler.handleSubmit(titleValue, "", null, fieldValues);
            return;
        }
        final String urlNameValue = getAndRemoveValue(fieldValues, FIELD_URLNAME);
        validateUrlName(urlNameValue, new AsyncCallback<String>() {

            /**
             * @see com.google.gwt.user.client.rpc.AsyncCallback#onFailure(java.lang.Throwable)
             */
            public void onFailure(Throwable caught) {

                // do nothing, this will never be called 
            }

            /**
             * @see com.google.gwt.user.client.rpc.AsyncCallback#onSuccess(java.lang.Object) 
             */
            public void onSuccess(String newUrlName) {

                hide();
                m_handler.handleSubmit(titleValue, newUrlName, null, fieldValues);
            }
        });
    }

    /**
     * Returns a localized message from the message bundle.<p>
     * 
     * @param key the message key
     * @param args the message parameters
     *  
     * @return the localized message 
     */
    protected String message(String key, Object... args) {

        return Messages.get().key(key, args);
    }

    /**
     * @see org.opencms.gwt.client.ui.input.form.CmsFormDialog#onClickCancel()
     */
    @Override
    protected void onClickCancel() {

        super.onClickCancel();
        m_handler.handleCancel();
    }

    /**
     * @see org.opencms.gwt.client.ui.input.form.CmsFormDialog#onClickOk()
     */
    @Override
    protected void onClickOk() {

        m_form.validate(new I_CmsValidationHandler() {

            /**
             * @see org.opencms.gwt.client.ui.input.I_CmsValidationHandler#onValidationComplete(boolean)
             */
            public void onValidationComplete(boolean validationSucceeded) {

                if (validationSucceeded) {
                    handleSubmit();
                }
            }
        });
    }

    /**
     * Sets the contents of the URL name field in the form.<p>
     * 
     * @param urlName the new URL name
     */
    protected void setUrlNameField(String urlName) {

        getForm().getField(FIELD_URLNAME).getWidget().setFormValueAsString(urlName);
    }

    /**
     * Shows an error message next to the URL name input field.<p>
     * 
     * @param message the message which should be displayed, or null if no message should be displayed 
     */
    protected void showUrlNameError(String message) {

        getForm().getField(FIELD_URLNAME).getWidget().setErrorMessage(message);
    }

    /**
     * Validates the url name, and if it is ok, executes another action and passes the translated url
     * name to it.<p>
     * 
     * @param urlName the url name to validate 
     * @param nextAction the action which should be executed if the validation turns out ok
     */
    protected void validateUrlName(String urlName, final AsyncCallback<String> nextAction) {

        showUrlNameError(null);
        if (CmsStringUtil.isEmptyOrWhitespaceOnly(urlName)) {
            // empty url name; don't bother with translating it
            showUrlNameError(message(Messages.GUI_URLNAME_CANT_BE_EMPTY_0));
            return;
        }
        CmsCoreProvider.get().translateUrlName(urlName, new AsyncCallback<String>() {

            /**
             * @see com.google.gwt.user.client.rpc.AsyncCallback#onFailure(java.lang.Throwable)
             */
            public void onFailure(Throwable caught) {

                // will never be executed; do nothing 
            }

            /**
             * @see com.google.gwt.user.client.rpc.AsyncCallback#onSuccess(java.lang.Object)
             */
            public void onSuccess(String newUrlName) {

                setUrlNameField(newUrlName);
                if (!m_handler.isPathAllowed(newUrlName)) {
                    showUrlNameError(message(Messages.GUI_URLNAME_ALREADY_EXISTS_0));
                } else if (nextAction != null) {
                    nextAction.onSuccess(newUrlName);
                }
            }
        });

    }

    /**
     * Helper method for creating the form field for selecting a template.<p>
     * 
     * @return the template form field 
     */
    private CmsBasicFormField createTemplateField() {

        String description = message(Messages.GUI_TEMPLATE_PROPERTY_DESC_0);
        String label = message(Messages.GUI_TEMPLATE_PROPERTY_TITLE_0);
        CmsTemplateSelectBox select = createTemplateSelector(m_controller.getData().getTemplates());
        return new CmsBasicFormField(FIELD_TEMPLATE, description, label, null, select);
    }

    /** 
     * Helper method for creating the form field for selecting whether the template should be inherited or not.<p>
     * 
     * @return the new form field 
     */
    private CmsBasicFormField createTemplateInheritField() {

        String description = "";
        String label = "";
        CmsCheckBox checkbox = new CmsCheckBox(message(Messages.GUI_TEMPLATE_INHERIT_0));
        CmsBasicFormField result = new CmsBasicFormField(
            FIELD_TEMPLATE_INHERIT_CHECKBOX,
            description,
            label,
            null,
            checkbox);
        return result;
    }

    /**
     * Helper method for creating the template selection widget.<p>
     * 
     * @param templates the map of available templates
     * 
     * @return the template selector widget 
     */
    private CmsTemplateSelectBox createTemplateSelector(Map<String, CmsSitemapTemplate> templates) {

        CmsTemplateSelectBox result = new CmsTemplateSelectBox();
        for (Map.Entry<String, CmsSitemapTemplate> templateEntry : templates.entrySet()) {
            CmsSitemapTemplate template = templateEntry.getValue();
            CmsTemplateSelectCell selectCell = new CmsTemplateSelectCell();
            selectCell.setTemplate(template);
            result.addOption(selectCell);
        }
        CmsTemplateSelectCell defaultCell = new CmsTemplateSelectCell();
        defaultCell.setTemplate(getDefaultTemplate());
        result.addOption(defaultCell);
        return result;
    }

    /**
     * Creates the text field for editing the title.<p>
     * 
     * @param entry the entry which is being edited
     *  
     * @return the newly created form field 
     */
    private CmsBasicFormField createTitleField(CmsClientSitemapEntry entry) {

        String description = message(Messages.GUI_TITLE_PROPERTY_DESC_0);
        String label = message(Messages.GUI_TITLE_PROPERTY_0);

        CmsBasicFormField result = new CmsBasicFormField(FIELD_TITLE, description, label, null, new CmsTextBox());
        String title = m_handler.getTitle();
        if (title == null) {
            title = "";
        }
        result.getWidget().setFormValueAsString(title);
        result.setValidator(new CmsNonEmptyValidator(Messages.get().key(Messages.GUI_TITLE_CANT_BE_EMPTY_0)));
        return result;
    }

    /**
     * Creates the text field for editing the URL name.<p>
     * 
     * @param entry the entry which is being edited
     *  
     * @return the newly created form field 
     */
    private CmsBasicFormField createUrlNameField(CmsClientSitemapEntry entry) {

        String description = message(Messages.GUI_URLNAME_PROPERTY_DESC_0);
        String label = message(Messages.GUI_URLNAME_PROPERTY_0);
        final CmsTextBox textbox = new CmsTextBox();
        textbox.addBlurHandler(new BlurHandler() {

            /**
             * @see com.google.gwt.event.dom.client.BlurHandler#onBlur(com.google.gwt.event.dom.client.BlurEvent)
             */
            public void onBlur(BlurEvent event) {

                validateUrlName(textbox.getText(), null);
            }
        });
        CmsBasicFormField result = new CmsBasicFormField(FIELD_URLNAME, description, label, null, textbox);
        String urlName = m_handler.getName();
        if (urlName == null) {
            urlName = "";
        }
        result.getWidget().setFormValueAsString(urlName);
        return result;
    }

    /**
     * Returns the template which should be used as the "use default" option in the template selector.<p>
     * 
     * @return the default template 
     */
    private CmsSitemapTemplate getDefaultTemplate() {

        CmsSitemapTemplate template = m_controller.getDefaultTemplate(m_handler.getEntry().getSitePath());
        // replace site path with empty string and title with "default" 
        String defaultTitle = message(Messages.GUI_DEFAULT_TEMPLATE_TITLE_0);
        return new CmsSitemapTemplate(
            defaultTitle,
            template.getDescription(),
            DEFAULT_TEMPLATE_VALUE,
            template.getImgPath());
    }

    /**
     * Helper method for extracting new values for the 'template' and 'template-inherited' properties from the
     * raw form data.<p>
     * 
     * @param fieldValues the string map produced by the form 
     * 
     * @return a pair containing the 'template' and 'template-inherit' property, in that order
     */
    private CmsPair<String, String> getTemplateProperties(Map<String, String> fieldValues) {

        String shouldInheritTemplateStr = getAndRemoveValue(fieldValues, FIELD_TEMPLATE_INHERIT_CHECKBOX);
        String template = fieldValues.get(CmsSitemapManager.Property.template.toString());
        if (template.equals(DEFAULT_TEMPLATE_VALUE)) {
            // return nulls to cause the properties to be deleted  
            return new CmsPair<String, String>(null, null);
        }

        // only inherit the template if checkbox is checked 
        String templateInherited = Boolean.parseBoolean(shouldInheritTemplateStr) ? template : null;
        return new CmsPair<String, String>(template, templateInherited);
    }

    /**
     * Helper method for removing hidden properties from a map of property configurations.<p>
     * 
     * The map passed into the method is not changed; a map which only contains the non-hidden
     * property definitions is returned.<p>
     * 
     * @param propConfig the property configuration 
     * 
     * @return the filtered property configuration 
     */
    private Map<String, CmsXmlContentProperty> removeHiddenProperties(Map<String, CmsXmlContentProperty> propConfig) {

        Map<String, CmsXmlContentProperty> result = new HashMap<String, CmsXmlContentProperty>();
        for (Map.Entry<String, CmsXmlContentProperty> entry : propConfig.entrySet()) {
            if (!m_controller.isHiddenProperty(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }
}