package org.vaadin.karttarekisteri;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.vaadin.Application;
import com.vaadin.terminal.gwt.server.HttpServletRequestListener;
import com.vaadin.ui.Window;

public class VApp extends Application implements
        HttpServletRequestListener {

    private Window window;

    private boolean initilized = false;

    @Override
    public void init() {
        window = new Window();
        setMainWindow(window);

    }

    private void showStartScreen(String focusedFeature) {

        window.setContent(new KmlDocumentViewer(focusedFeature));

    }

    @Override
    public void onRequestStart(HttpServletRequest request,
            HttpServletResponse response) {
        if (!initilized && request.getParameter("sh") != null) {

            String parameter = request.getParameter("fr");
            showStartScreen(parameter);

            initilized = true;
        }
    }

    @Override
    public void onRequestEnd(HttpServletRequest request,
            HttpServletResponse response) {
        // TODO Auto-generated method stub

    }

}
