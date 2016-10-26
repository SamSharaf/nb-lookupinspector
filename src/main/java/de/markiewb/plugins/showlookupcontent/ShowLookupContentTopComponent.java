/**
 * Copyright 2016 markiewb
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package de.markiewb.plugins.showlookupcontent;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JEditorPane;
import javax.swing.JTextArea;
import javax.swing.text.AbstractDocument;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.cookies.EditorCookie;
import org.openide.text.NbDocument;
import org.openide.util.Lookup.Result;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle.Messages;
import org.openide.util.Utilities;
import org.openide.windows.TopComponent;

@TopComponent.Description(
        preferredID = "showlookupcontentTopComponent",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS)
@TopComponent.Registration(mode = "output", openAtStartup = false)
@ActionID(category = "Window/Debug", id = "de.markiewb.plugins.showlookupcontent.ShowLookupContentTopComponent")
@ActionReference(path = "Menu/Window/Debug", position = 2000)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_showlookupcontentAction",
        preferredID = "showlookupcontentTopComponent")
@Messages({
    "CTL_showlookupcontentAction=Lookup content inspector",
    "CTL_showlookupcontentTopComponent=Lookup content inspector",
    "HINT_showlookupcontentTopComponent=Lookup content inspector. It shows the content of the current lookup."
})
public final class ShowLookupContentTopComponent extends TopComponent implements LookupListener {

    private static final Logger LOG = Logger.getLogger(ShowLookupContentTopComponent.class.getName());

    public ShowLookupContentTopComponent() {
        initComponents();
        setName(Bundle.CTL_showlookupcontentTopComponent());
        setToolTipText(Bundle.HINT_showlookupcontentTopComponent());

        lookupResult = Utilities.actionsGlobalContext().lookupResult(Object.class);

    }

    public void formatRecursive(int depth, final Class<? extends Object> aClass, List<String> list) {
        if (null == aClass) {
            return;
        }
        // everything is an Object, so do not print
        if (Object.class.equals(aClass)) {
            return;
        }
        String indentedText = getIndentedText(depth, aClass.toString());

        list.add(indentedText);

        Class<? extends Object> superclass = aClass.getSuperclass();
        if (null != superclass) {
            formatRecursive(depth + 1, superclass, list);
        }
        Class<?>[] interfaces = aClass.getInterfaces();
        if (null != interfaces && interfaces.length > 0) {
            for (Class<?> aInterface : interfaces) {
                formatRecursive(depth + 1, aInterface, list);
            }
        }
    }

    public List<String> getContentFromLookup(Collection<? extends Object> allInstances) {
        List<String> list = new ArrayList<String>();
        list.add("# of instances: " + allInstances.size());
        list.add("");
        list.add("--Classes in lookup--");
        for (Object node : sortByClassName(allInstances)) {
            final Class<? extends Object> aClass = node.getClass();
            list.add(String.format("%-70s %s", aClass.toString().trim(), node.toString().trim()));
        }
        return list;
    }

    public List<String> getContentFromLookupWithHierarchy(Collection<? extends Object> allInstances) {

        List<String> list = new ArrayList<String>();
        list.add("# of instances: " + allInstances.size());
        list.add("");
        list.add("--Classes in lookup incl. their hierarchy--");
        for (Object node : sortByClassName(allInstances)) {
            final Class<? extends Object> aClass = node.getClass();
            int depth = 0;
            formatRecursive(depth, aClass, list);
        }
        return list;
    }

    public List<String> getDocumentProperties() {
        List<String> list = new ArrayList<String>();
        EditorCookie editorCookie = Utilities.actionsGlobalContext().lookup(EditorCookie.class);
        JTextComponent comp = null;
        if (null != editorCookie && NbDocument.findRecentEditorPane(editorCookie) != null) {

            comp = NbDocument.findRecentEditorPane(editorCookie);
        }
        if (null != comp) {
            Document document = comp.getDocument();
            if (null != document) {

                if (document instanceof AbstractDocument) {
                    AbstractDocument doc = (AbstractDocument) document;
                    list.add(getIndentedText(0, "" + document));
                    list.add("");

                    Dictionary<Object, Object> documentProperties = doc.getDocumentProperties();
                    Enumeration<Object> keys = documentProperties.keys();
                    //Convert to sorted map
                    Map<String, String> map = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);

                    while (keys.hasMoreElements()) {
                        Object key = keys.nextElement();
                        Object value = documentProperties.get(key);
                        if ("interface java.lang.CharSequence".equals("" + key)) {
                            value = "DOCUMENT CONTENT WILL NOT BE DISPLAYED";
                        }

                        map.put("" + key, "" + value);
                    }

                    for (Map.Entry<String, String> entry : map.entrySet()) {
                        String key = entry.getKey();
                        String value = entry.getValue();
                        list.add(getIndentedText(0, key + "=" + value));
                    }

                }
            }
        }
        return list;
    }

    public List<String> getTextEditorClientProperties() {
        List<String> list = new ArrayList<String>();
        EditorCookie editorCookie = Utilities.actionsGlobalContext().lookup(EditorCookie.class);
        JEditorPane comp = null;
        if (null != editorCookie && NbDocument.findRecentEditorPane(editorCookie) != null) {

            comp = NbDocument.findRecentEditorPane(editorCookie);
        }
        if (null != comp) {
            Map<String, String> props = getClientProperties(comp);
            if (null != props) {

                //Convert to sorted map
                Map<String, String> map = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
                map.putAll(props);
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    list.add(getIndentedText(0, key + "=" + value));
                }
            }
        }
        return list;
    }

    public String getIndentedText(int depth, final String text) {
        int indent = depth * 8;
        final String indentedText = indent(indent, text);
        return indentedText;
    }

    @Override
    public void resultChanged(LookupEvent ev) {
        Collection<? extends Object> allInstances = lookupResult.allInstances();
        final TopComponent tc = TopComponent.getRegistry().getActivated();
        txtTopComponent.setText("" + tc + "\n");

        if (tc != this) {
            setTextForTextArea(lookupTextArea, getContentFromLookup(allInstances));
            setTextForTextArea(lookupHierarchyTextArea, getContentFromLookupWithHierarchy(allInstances));
            setTextForTextArea(documentPropertiesTextArea, getDocumentProperties());
            setTextForTextArea(jcomponentClientPropertiesTextArea, getTextEditorClientProperties());
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    private Map<String, String> getClientProperties(JTextComponent comp) {
        Map<String, String> result = new HashMap<String, String>();

        try {
            //javax.swing.ArrayTable javax.swing.JComponent.getClientProperties()
            Method methodgetClientProperties = javax.swing.JComponent.class.getDeclaredMethod("getClientProperties");
            methodgetClientProperties.setAccessible(true); //remove final
            Object arrayTable = methodgetClientProperties.invoke(comp);

            //Object[] javax.swing.ArrayTable.getKeys(Object[])
            Method methodgetKeys = arrayTable.getClass().getDeclaredMethod("getKeys", Object[].class);
            methodgetKeys.setAccessible(true); //remove final
            Object[] keys = (Object[]) methodgetKeys.invoke(arrayTable, (Object) null);

            for (Object key : keys) {
                Object value = comp.getClientProperty(key);
                result.put("" + key, "" + value);
            }

            return result;
        } catch (Exception ex) {
            //ignore
            LOG.log(Level.INFO, "Cannot get getClientProperties.", ex);
        }

        return result;
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSplitPane1 = new javax.swing.JSplitPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtTopComponent = new javax.swing.JTextArea();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        lookupPanel = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        lookupTextArea = new javax.swing.JTextArea();
        lookupWithHierarchyPanel = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        lookupHierarchyTextArea = new javax.swing.JTextArea();
        documentPropertiesPanel = new javax.swing.JPanel();
        jScrollPane6 = new javax.swing.JScrollPane();
        documentPropertiesTextArea = new javax.swing.JTextArea();
        jcomponentClientPropertiesPanel = new javax.swing.JPanel();
        jScrollPane5 = new javax.swing.JScrollPane();
        jcomponentClientPropertiesTextArea = new javax.swing.JTextArea();
        jLabel1 = new javax.swing.JLabel();

        jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        txtTopComponent.setColumns(20);
        txtTopComponent.setFont(new java.awt.Font("Courier New", 0, 12)); // NOI18N
        txtTopComponent.setRows(5);
        jScrollPane1.setViewportView(txtTopComponent);

        jSplitPane1.setLeftComponent(jScrollPane1);

        lookupPanel.setLayout(new java.awt.BorderLayout());

        lookupTextArea.setColumns(20);
        lookupTextArea.setRows(5);
        jScrollPane2.setViewportView(lookupTextArea);

        lookupPanel.add(jScrollPane2, java.awt.BorderLayout.CENTER);

        jTabbedPane1.addTab(org.openide.util.NbBundle.getMessage(ShowLookupContentTopComponent.class, "ShowLookupContentTopComponent.lookupPanel.TabConstraints.tabTitle"), lookupPanel); // NOI18N

        lookupWithHierarchyPanel.setLayout(new java.awt.BorderLayout());

        lookupHierarchyTextArea.setColumns(20);
        lookupHierarchyTextArea.setRows(5);
        jScrollPane4.setViewportView(lookupHierarchyTextArea);

        lookupWithHierarchyPanel.add(jScrollPane4, java.awt.BorderLayout.CENTER);

        jTabbedPane1.addTab(org.openide.util.NbBundle.getMessage(ShowLookupContentTopComponent.class, "ShowLookupContentTopComponent.lookupWithHierarchyPanel.TabConstraints.tabTitle"), lookupWithHierarchyPanel); // NOI18N

        documentPropertiesPanel.setLayout(new java.awt.BorderLayout());

        documentPropertiesTextArea.setColumns(20);
        documentPropertiesTextArea.setRows(5);
        jScrollPane6.setViewportView(documentPropertiesTextArea);

        documentPropertiesPanel.add(jScrollPane6, java.awt.BorderLayout.CENTER);

        jTabbedPane1.addTab(org.openide.util.NbBundle.getMessage(ShowLookupContentTopComponent.class, "ShowLookupContentTopComponent.documentPropertiesPanel.TabConstraints.tabTitle"), documentPropertiesPanel); // NOI18N

        jcomponentClientPropertiesPanel.setLayout(new java.awt.BorderLayout());

        jcomponentClientPropertiesTextArea.setColumns(20);
        jcomponentClientPropertiesTextArea.setRows(5);
        jScrollPane5.setViewportView(jcomponentClientPropertiesTextArea);

        jcomponentClientPropertiesPanel.add(jScrollPane5, java.awt.BorderLayout.CENTER);

        jTabbedPane1.addTab(org.openide.util.NbBundle.getMessage(ShowLookupContentTopComponent.class, "ShowLookupContentTopComponent.jcomponentClientPropertiesPanel.TabConstraints.tabTitle"), jcomponentClientPropertiesPanel); // NOI18N

        jSplitPane1.setBottomComponent(jTabbedPane1);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(ShowLookupContentTopComponent.class, "ShowLookupContentTopComponent.jLabel1.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addComponent(jSplitPane1, javax.swing.GroupLayout.Alignment.TRAILING)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSplitPane1))
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel documentPropertiesPanel;
    private javax.swing.JTextArea documentPropertiesTextArea;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JPanel jcomponentClientPropertiesPanel;
    private javax.swing.JTextArea jcomponentClientPropertiesTextArea;
    private javax.swing.JTextArea lookupHierarchyTextArea;
    private javax.swing.JPanel lookupPanel;
    private javax.swing.JTextArea lookupTextArea;
    private javax.swing.JPanel lookupWithHierarchyPanel;
    private javax.swing.JTextArea txtTopComponent;
    // End of variables declaration//GEN-END:variables
    Result<Object> lookupResult;

    @Override
    public void componentOpened() {

        lookupResult.addLookupListener(this);
    }

    @Override
    public void componentClosed() {
        lookupResult.removeLookupListener(this);
    }

    private String indent(int indent, String text) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            sb.append(" ");
        }
        sb.append(text);
        return sb.toString();
    }

    private void setTextForTextArea(JTextArea textArea, List<String> list) {

        textArea.setText("");

        for (String text : list) {
            textArea.append(text + "\n");
        }
        textArea.setCaretPosition(0);
    }

    private List<Object> sortByClassName(Collection<? extends Object> allInstances) {
        List<Object> sortedInstances = new ArrayList<Object>(allInstances);
        Collections.sort(sortedInstances, new Comparator<Object>() {
            public int compare(Object o1, Object o2) {
                return o1.getClass().toString().compareTo(o2.getClass().toString());
            }
        });
        return sortedInstances;
    }

}
