// Prevayler, The Free-Software Prevalence Layer
// Copyright 2001-2006 by Klaus Wuestefeld
//
// This library is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.
//
// Prevayler is a trademark of Klaus Wuestefeld.
// See the LICENSE file for license details.

package org.prevayler.demos.demo2.gui;

import org.prevayler.Listener;
import org.prevayler.Prevayler;
import org.prevayler.demos.demo2.business.Bank;
import org.prevayler.demos.demo2.business.transactions.AccountDeletion;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;

class AllAccountsFrame extends JInternalFrame implements Listener<BankEvent> {

    private static final long serialVersionUID = -9182376858708585231L;

    private final Prevayler<Bank> _prevayler;

    private final JList accountList;

    AllAccountsFrame(Prevayler<Bank> prevayler, Container container) {
        super("All Accounts", true); // true means resizable.
        _prevayler = prevayler;

        accountList = new JList();
        accountList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        prevayler.register(BankEvent.class, this);
        refreshAccounts();

        container.add(this);
        getContentPane().add(new JScrollPane(accountList), BorderLayout.CENTER);
        getContentPane().add(accountButtons(), BorderLayout.SOUTH);

        setBounds(10, 10, 330, 240);
        show();
    }

    private void refreshAccounts() {
        accountList.setListData(_prevayler.execute(new AccountInfoQuery()));
    }

    public void handle(@SuppressWarnings("unused") BankEvent event) {
        refreshAccounts();
    }

    private JPanel accountButtons() {
        JPanel result = new JPanel();

        result.add(new JButton(new AccountCreation()));
        result.add(new JButton(new AccountEditAction()));
        result.add(new JButton(new AccountDeleteAction()));

        return result;
    }

    class AccountCreation extends AbstractAction {

        private static final long serialVersionUID = 9182061237235826622L;

        AccountCreation() {
            super("Create");
        }

        public void actionPerformed(@SuppressWarnings("unused") ActionEvent e) {
            new NewAccountFrame(_prevayler, getDesktopPane());
        }

    }

    abstract class SelectedAccountAction extends RobustAction implements ListSelectionListener {

        SelectedAccountAction(String name) {
            super(name);
            refreshEnabled();
            accountList.addListSelectionListener(this);
        }

        private void refreshEnabled() {
            this.setEnabled(accountList.getSelectedValue() != null);
        }

        public void valueChanged(@SuppressWarnings("unused") ListSelectionEvent event) {
            refreshEnabled();
        }

        @Override protected void action() throws Exception {
            action((AccountInfo) accountList.getSelectedValue());
        }

        abstract void action(AccountInfo accountInfo) throws Exception;

    }

    class AccountEditAction extends SelectedAccountAction {

        private static final long serialVersionUID = 5901968097767524191L;

        AccountEditAction() {
            super("Edit");
        }

        @Override void action(AccountInfo accountInfo) throws Exception {
            new AccountEditFrame(accountInfo.getNumber(), _prevayler, getDesktopPane());
        }
    }

    class AccountDeleteAction extends SelectedAccountAction {

        private static final long serialVersionUID = -1320330350155595965L;

        AccountDeleteAction() {
            super("Delete");
        }

        @Override void action(AccountInfo accountInfo) throws Exception {
            int option = JOptionPane.showConfirmDialog(null, "Delete selected account?", "Account Deletion", JOptionPane.YES_NO_OPTION);
            if (option != JOptionPane.YES_OPTION)
                return;

            _prevayler.execute(new AccountDeletion(accountInfo.getNumber()));
        }

    }

}