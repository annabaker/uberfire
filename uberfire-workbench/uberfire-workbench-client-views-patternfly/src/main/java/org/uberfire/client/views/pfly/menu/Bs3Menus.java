package org.uberfire.client.views.pfly.menu;

import java.util.ArrayDeque;
import java.util.Deque;

import org.gwtbootstrap3.client.ui.Anchor;
import org.gwtbootstrap3.client.ui.AnchorListItem;
import org.gwtbootstrap3.client.ui.ListItem;
import org.gwtbootstrap3.client.ui.base.AbstractListItem;
import org.jboss.errai.security.shared.api.identity.User;
import org.uberfire.client.menu.AuthFilterMenuVisitor;
import org.uberfire.security.authz.AuthorizationManager;
import org.uberfire.workbench.model.menu.EnabledStateChangeListener;
import org.uberfire.workbench.model.menu.MenuCustom;
import org.uberfire.workbench.model.menu.MenuGroup;
import org.uberfire.workbench.model.menu.MenuItem;
import org.uberfire.workbench.model.menu.MenuItemCommand;
import org.uberfire.workbench.model.menu.MenuItemPlain;
import org.uberfire.workbench.model.menu.MenuVisitor;
import org.uberfire.workbench.model.menu.Menus;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.IsWidget;

/**
 * Utilities for building Bootstrap 3 menus from UberFire menu descriptions.
 */
public class Bs3Menus {

    /**
     * Constructs views for all the menu groups and menu items described by the given menu description, adding these
     * views to the given top-level container.
     *
     * @param menus the description of the menus to build. Not null.
     * @param authzManager the current authorization manager for the application. Not null.
     * @param identity the identity of the user who will see the menus. Not null.
     * @param topLevelWidget the container to all all the menu items to. Not null.
     */
    public static void constructMenuView( final Menus menus,
                                          final AuthorizationManager authzManager,
                                          final User identity,
                                          final HasMenuItems topLevelWidget ) {

        MenuVisitor viewBuilder = new MenuVisitor() {

            Deque<HasMenuItems> parentMenus = new ArrayDeque<HasMenuItems>();

            @Override
            public boolean visitEnter( Menus menus ) {
                parentMenus.push( topLevelWidget );
                return true;
            }

            @Override
            public void visitLeave( Menus menus ) {
                parentMenus.pop();
            }

            @Override
            public boolean visitEnter( MenuGroup menuGroup ) {
                parentMenus.push( new Bs3DropDownMenu( menuGroup.getCaption() ) );
                return true;
            }

            @Override
            public void visitLeave( MenuGroup menuGroup ) {
                HasMenuItems finishedMenu = parentMenus.pop();
                parentMenus.peek().addMenuItem( (AbstractListItem) finishedMenu );
            }

            @Override
            public void visit( MenuCustom<?> menuCustom ) {
                IsWidget customMenuItem = (IsWidget) menuCustom.build();
                AbstractListItem view;
                if ( customMenuItem instanceof AbstractListItem ) {
                    view = (AbstractListItem) customMenuItem;
                } else {
                    Anchor anchor = new Anchor();
                    anchor.add( customMenuItem );

                    ListItem container = new ListItem();
                    container.add( anchor );

                    view = container;
                }
                setupEnableDisable( menuCustom,
                                    view );
                parentMenus.peek().addMenuItem( view );
            }

            @Override
            public void visit( final MenuItemCommand menuItemCommand ) {
                final AnchorListItem listItem = new AnchorListItem( menuItemCommand.getCaption() );
                setupEnableDisable( menuItemCommand,
                                    listItem );
                listItem.addClickHandler( new ClickHandler() {
                    @Override
                    public void onClick( final ClickEvent event ) {
                        menuItemCommand.getCommand().execute();
                    }
                } );
                parentMenus.peek().addMenuItem( listItem );
            }

            @Override
            public void visit( MenuItemPlain menuItemPlain ) {
                AnchorListItem view = new AnchorListItem( menuItemPlain.getCaption() );
                setupEnableDisable( menuItemPlain,
                                    view );
                parentMenus.peek().addMenuItem( view );
            }

            /**
             * Sets up the enabled/disabled state of the view widget, and installs a listener on the model to keep the
             * widget's enabled state in sync with it.
             *
             * @param model
             *            the description of the menu item to get the current enabled state from, and to subscibe to for
             *            future changes.
             * @param view
             *            the widget that provides a view of the given model.
             */
            private void setupEnableDisable( final MenuItem model,
                                             final AbstractListItem view ) {
                view.setEnabled( model.isEnabled() );
                model.addEnabledStateChangeListener( new EnabledStateChangeListener() {
                    @Override
                    public void enabledStateChanged( final boolean enabled ) {
                        view.setEnabled( enabled );
                    }
                } );
            }

        };

        menus.accept( new AuthFilterMenuVisitor( authzManager, identity, viewBuilder ) );
    }

}