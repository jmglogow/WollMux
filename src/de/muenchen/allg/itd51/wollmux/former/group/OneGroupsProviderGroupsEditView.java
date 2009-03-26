/*
 * Dateiname: OneGroupsProviderGroupsEditView.java
 * Projekt  : WollMux
 * Funktion : L�sst die Liste der Gruppen eines GroupsProvider bearbeiten.
 * 
 * Copyright (c) 2008 Landeshauptstadt M�nchen
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL), 
 * version 1.0.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * European Union Public Licence for more details.
 *
 * You should have received a copy of the European Union Public Licence
 * along with this program. If not, see 
 * http://ec.europa.eu/idabc/en/document/7330
 *
 * �nderungshistorie:
 * Datum      | Wer | �nderungsgrund
 * -------------------------------------------------------------------
 * 13.03.2009 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD D.10)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.former.group;

import java.awt.GridLayout;
import java.util.Iterator;
import java.util.Set;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import de.muenchen.allg.itd51.wollmux.former.IDManager;
import de.muenchen.allg.itd51.wollmux.former.IDManager.ID;
import de.muenchen.allg.itd51.wollmux.former.IDManager.IDChangeListener;
import de.muenchen.allg.itd51.wollmux.former.view.View;

/**
 * L�sst die Liste der Gruppen eines {@link GroupsProvider} bearbeiten.
 * 
 * @author Matthias Benkmann (D-III-ITD-D101)
 */
public class OneGroupsProviderGroupsEditView implements View
{
  /**
   * Der Obercontainer dieser View.
   */
  private JPanel myPanel;

  /**
   * Die Liste mit allen Gruppen aus {@link #groupModelList}, wobei die Gruppen aus
   * {@link #groupsProvider} selektiert sind.
   */
  private JList myList;

  /**
   * Das {@link ListModel} zu {@link #myList}.
   */
  private DefaultListModel listModel;

  /**
   * Wessen Gruppen werden angezeigt und bearbeitet.
   */
  private GroupsProvider groupsProvider;

  /**
   * Welche Gruppen stehen zur Auswahl.
   */
  private GroupModelList groupModelList;

  /**
   * Listener auf model und groupModelList, der Hinzuf�gen und entfernen von Gruppen
   * beobachtet.
   */
  private MyListener myListener;

  /**
   * Wird tempor�r auf true gesetzt, w�hrend einer Aktion die rekursives Aufrufen des
   * Listeners erzeugen k�nnte, um diese Rekursion zu durchbrechen.
   */
  boolean recursion = false;

  public OneGroupsProviderGroupsEditView(GroupsProvider groupsProvider,
      GroupModelList groupModelList)
  {
    this.groupsProvider = groupsProvider;
    myListener = new MyListener();
    groupsProvider.addGroupsChangedListener(myListener);

    this.groupModelList = groupModelList;
    groupModelList.addListener(myListener);

    myPanel = new JPanel(new GridLayout(1, 1));
    listModel = new DefaultListModel();
    myList = new JList(listModel);
    myList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

    JScrollPane scrollPane = new JScrollPane(myList);
    myPanel.add(scrollPane);

    for (GroupModel model : groupModelList)
    {
      IDManager.ID id = model.getID();
      listModel.addElement(id);
      id.addIDChangeListener(myListener);
    }

    for (IDManager.ID id : groupsProvider)
    {
      if (!listModel.contains(id)) listModel.addElement(id);
    }

    Set<IDManager.ID> selected = groupsProvider.getGroups();
    int[] indices = new int[selected.size()];
    int i = 0;
    for (IDManager.ID id : selected)
    {
      indices[i++] = listModel.indexOf(id);
    }

    myList.setSelectedIndices(indices);

    myList.addListSelectionListener(myListener);
  }

  public JComponent JComponent()
  {
    return myPanel;
  }

  private class MyListener implements GroupModelList.ItemListener,
      GroupsProvider.GroupsChangedListener, ListSelectionListener, IDChangeListener
  {

    /**
     * Zu groupModelList (d.h. zur Menge aller verf�gbaren Gruppen) wurde eine Gruppe
     * hinzugef�gt.
     */
    public void itemAdded(GroupModel model, int index)
    {
      if (recursion) return;
      recursion = true;
      IDManager.ID id = model.getID();
      if (!listModel.contains(id)) listModel.addElement(id);
      recursion = false;
    }

    /**
     * Aus groupModelList (d.h. aus der Menge aller verf�gbaren Gruppen) wurde eine
     * Gruppe entfernt.
     */
    public void itemRemoved(GroupModel model, int index)
    {
      if (recursion) return;
      recursion = true;
      IDManager.ID groupID = model.getID();
      listModel.removeElement(groupID);
      groupsProvider.removeGroup(groupID);
      recursion = false;
    }

    /**
     * Zu groupsProvider (d.h. der Liste der selektierten Gruppen) wurde eine Gruppe
     * hinzugef�gt.
     */
    public void groupAdded(ID groupID)
    {
      if (recursion) return;
      recursion = true;
      int index = listModel.indexOf(groupID);
      if (index >= 0) myList.getSelectionModel().addSelectionInterval(index, index);

      recursion = false;
    }

    /**
     * Aus groupsProvider (d.h. aus der Liste der selektierten Gruppen) wurde eine
     * Gruppe entfernt.
     */
    public void groupRemoved(ID groupID)
    {
      if (recursion) return;
      recursion = true;
      int index = listModel.indexOf(groupID);
      if (index >= 0)
        myList.getSelectionModel().removeSelectionInterval(index, index);
      recursion = false;
    }

    /**
     * Die Selektion wurde in der GUI ge�ndert.
     */
    public void valueChanged(ListSelectionEvent e)
    {
      if (recursion) return;
      if (e.getValueIsAdjusting()) return;
      recursion = true;

      Object[] selected = myList.getSelectedValues();
      Iterator<IDManager.ID> iter = groupsProvider.iterator();
      while (iter.hasNext())
      {
        IDManager.ID id = iter.next();
        found: do
        {
          for (Object o : selected)
          {
            if (o.equals(id)) break found;
          }

          iter.remove();
        } while (false);
      }

      for (Object o : selected)
      {
        for (GroupModel model : groupModelList)
        {
          IDManager.ID id = model.getID();
          if (o.equals(id)) groupsProvider.addGroup(id);
        }
      }

      recursion = false;
    }

    /**
     * Die ID einer der Gruppen aus groupModelList (d.h. der Liste aller verf�gbaren
     * Gruppen) sich ge�ndert.
     */
    public void idHasChanged(ID id)
    {}
  }
}
