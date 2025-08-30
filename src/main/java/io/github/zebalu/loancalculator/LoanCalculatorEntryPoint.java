/*
    Copyright 2025 Balázs Zaicsek

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

 */
package io.github.zebalu.loancalculator;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.*;

import java.util.ArrayList;
import java.util.List;

public class LoanCalculatorEntryPoint implements EntryPoint {

    private final ListBox languageBox = new ListBox();
    private final ListBox currencyBox = new ListBox();
    private final TextBox loanAmount = new TextBox();
    private final TextBox interestRate = new TextBox();
    private final TextBox yearsAmount = new TextBox();
    private final DivElement tableDiv = Document.get().createDivElement();
    private JavaScriptObject formatterConfig;
    private String languageCode = "hu-HU";
    private String currencyCode = "HUF";
    private final ClickHandler clickHandler = evt -> displayLoanCalculations();
    private final ChangeHandler formatChangeHandler = evt -> updateLanguageSettings();


    public void onModuleLoad() {
        addCSS();
        tableDiv.addClassName("result");
        createFormatterConfig();

        VerticalPanel mainForm = new VerticalPanel();
        createFormatPanel(mainForm);
        createLoanPanel(mainForm);
        createInterestPanel(mainForm);
        createYearsPanel(mainForm);
        Button calculateButton = new Button("Calculate");
        mainForm.add(calculateButton);
        calculateButton.addClickHandler(clickHandler);
        RootPanel.get().add(mainForm);
        RootPanel.get().getElement().appendChild(tableDiv);
    }

    private static void addCSS() {
        var e = DOM.createElement("style");
        e.setInnerText("""
                .result > table, .result > table td {
                     border: 1px solid black;
                     border-collapse: collapse;
                     padding: 10px;
                 }
                 body {
                     overscroll-behavior: contain;
                 }""");
        RootPanel.get().getElement().appendChild(e);
    }

    private void createFormatPanel(VerticalPanel mainForm) {
        HorizontalPanel formatPanel = new HorizontalPanel();
        formatPanel.setSpacing(10);
        formatPanel.setWidth("100%");
        Label langLabel = new Label("Format");
        languageBox.addItem("Hungarian", "hu-HU");
        languageBox.addItem("ENG/USA", "en-US");
        languageBox.addItem("ENG/GB", "en-GB");
        languageBox.addItem("German", "de-DE");
        languageBox.setSelectedIndex(0);
        formatPanel.add(langLabel);
        formatPanel.add(languageBox);
        Label currencyLabel = new Label("Currency");
        currencyBox.addItem("HUF", "HUF");
        currencyBox.addItem("USD", "USD");
        currencyBox.addItem("GBP", "GBP");
        currencyBox.addItem("EUR", "EUR");
        currencyBox.setSelectedIndex(0);
        formatPanel.add(currencyLabel);
        formatPanel.add(currencyBox);
        languageBox.addChangeHandler(formatChangeHandler);
        currencyBox.addChangeHandler(formatChangeHandler);
        mainForm.add(formatPanel);
    }

    private void createLoanPanel(VerticalPanel mainForm) {
        HorizontalPanel loanPanel = new HorizontalPanel();
        loanPanel.setSpacing(10);
        loanPanel.setWidth("100%");
        Label loanLabel = new Label("Loan");
        loanAmount.getElement().setAttribute("type", "number");
        loanAmount.getElement().setAttribute("value", "50000000");
        loanPanel.add(loanLabel);
        loanPanel.add(loanAmount);
        mainForm.add(loanPanel);
    }

    private void createInterestPanel(VerticalPanel mainForm) {
        HorizontalPanel interestPanel = new HorizontalPanel();
        interestPanel.setSpacing(10);
        interestPanel.setWidth("100%");
        Label interestLabel = new Label("Interest (%)");
        interestRate.getElement().setAttribute("type", "number");
        interestRate.getElement().setAttribute("value", "3.00");
        interestRate.getElement().setAttribute("step", "0.01");
        interestPanel.add(interestLabel);
        interestPanel.add(interestRate);
        mainForm.add(interestPanel);
    }

    private void createYearsPanel(VerticalPanel mainForm) {
        HorizontalPanel yearsPanel = new HorizontalPanel();
        yearsPanel.setSpacing(10);
        yearsPanel.setWidth("100%");
        Label yearsLabel = new Label("Years");
        yearsAmount.getElement().setAttribute("type", "number");
        yearsAmount.getElement().setAttribute("value", "25");
        yearsPanel.add(yearsLabel);
        yearsPanel.add(yearsAmount);
        mainForm.add(yearsPanel);
    }

    private Element createParagraph(String text) {
        Element p = DOM.createElement("p");
        p.setInnerText(text);
        return p;
    }

    private void createFormatterConfig() {
        JSONObject formatterConfig = new JSONObject();
        formatterConfig.put("style", new JSONString("currency"));
        formatterConfig.put("currency", new JSONString(currencyCode));
        this.formatterConfig = formatterConfig.getJavaScriptObject();
    }

    record Month(int month, double loanLeft, double loanPayedInMonth, double interestLeft,
                 double interestPayedInMonth) {
    }

    private String formatNumber(double number) {
        return jsFormat2(number, languageCode, formatterConfig);
    }

    private void displayLoanCalculations() {
        int loanAmount = Integer.parseInt(LoanCalculatorEntryPoint.this.loanAmount.getValue());
        double interestRate = Double.parseDouble(LoanCalculatorEntryPoint.this.interestRate.getValue());
        int yearsAmount = Integer.parseInt(LoanCalculatorEntryPoint.this.yearsAmount.getValue());
        int months = yearsAmount * 12;
        double monthlyInterestRate = interestRate / 100.0 / 12.0;
        double monthlyPayable = Math.round(
                loanAmount *
                        ((monthlyInterestRate * Math.pow((1 + monthlyInterestRate), months)) /
                                (Math.pow(1 + monthlyInterestRate, months) - 1))
        );
        double fullPayback = monthlyPayable * months;
        double fullInterest = fullPayback - loanAmount;
        List<Month> monthList = generateMonthDistribution(months, loanAmount, fullInterest, monthlyInterestRate, monthlyPayable);
        FlexTable flexTable = generateTableOfMonths(monthList);
        updateTableDisplay(monthlyPayable, fullPayback, fullInterest, flexTable);
    }

    private void updateTableDisplay(double monthlyPayable, double fullPayback, double fullInterest, FlexTable flexTable) {
        tableDiv.removeAllChildren();
        tableDiv.appendChild(createParagraph("Payed monthly: " + formatNumber(monthlyPayable)));
        tableDiv.appendChild(createParagraph("Full payback: " + formatNumber(fullPayback)));
        tableDiv.appendChild(createParagraph("Full interest: " + formatNumber(fullInterest)));
        tableDiv.appendChild(flexTable.getElement());
    }

    private FlexTable generateTableOfMonths(List<Month> monthList) {
        FlexTable flexTable = new FlexTable();
        flexTable.setText(0, 0, "Month");
        flexTable.setText(0, 1, "Loan left");
        flexTable.setText(0, 2, "Loan payed in month");
        flexTable.setText(0, 3, "Interest left");
        flexTable.setText(0, 4, "Interest payed in month");
        for (int r = 0; r < monthList.size(); ++r) {
            flexTable.setText(r + 1, 0, Integer.toString(monthList.get(r).month()));
            flexTable.setText(r + 1, 1, formatNumber(monthList.get(r).loanLeft()));
            flexTable.setText(r + 1, 2, formatNumber(monthList.get(r).loanPayedInMonth()));
            flexTable.setText(r + 1, 3, formatNumber(monthList.get(r).interestLeft()));
            flexTable.setText(r + 1, 4, formatNumber(monthList.get(r).interestPayedInMonth()));
        }
        return flexTable;
    }

    private void updateLanguageSettings() {
        languageCode = languageBox.getSelectedValue();
        currencyCode = currencyBox.getSelectedValue();
        createFormatterConfig();
    }

    private static List<Month> generateMonthDistribution(int months, int loanAmount, double fullInterest, double monthlyInterestRate, double monthlyPayable) {
        List<Month> monthList = new ArrayList<>(months);
        double lonaLeft = loanAmount;
        double interestLeft = fullInterest;
        for (int i = 1; i <= months; ++i) {
            if (i < months) {
                double interestToPay = Math.round(lonaLeft * monthlyInterestRate);
                double loanToPay = monthlyPayable - interestToPay;
                monthList.add(new Month(i, lonaLeft - loanToPay, loanToPay, interestLeft - interestToPay, interestToPay));
                lonaLeft -= loanToPay;
                interestLeft -= interestToPay;
            } else {
                monthList.add(new Month(i, 0, lonaLeft, 0, interestLeft));
            }
        }
        return monthList;
    }

    public static native String jsFormat2(Number data, String language, JavaScriptObject config) /*-{
          var formatter = new Intl.NumberFormat(language, config);
          return formatter.format(data);
    }-*/;

}