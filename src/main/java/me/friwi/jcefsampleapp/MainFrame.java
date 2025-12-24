// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package me.friwi.jcefsampleapp;

import me.friwi.jcefmaven.*;
import org.cef.CefApp;
import org.cef.CefApp.CefAppState;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefFocusHandlerAdapter;
import org.cef.handler.CefMessageRouterHandlerAdapter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;

/**
 * This is a simple example application using JCEF. It displays a JFrame with a
 * JTextField at its top and a CefBrowser in its center. The JTextField is used
 * to enter and assign an URL to the browser UI. No additional handlers or
 * callbacks are used in this example.
 * <p>
 * The number of used JCEF classes is reduced (nearly) to its minimum and should
 * assist you to get familiar with JCEF.
 * <p>
 * For a more feature complete example have also a look onto the example code
 * within the package "tests.detailed".
 */
public class MainFrame extends JFrame {
	private static final long serialVersionUID = -5570653778104813836L;
	private final JTextField address_;
	private final CefApp cefApp_;
	private final CefClient client_;
	private final CefBrowser browser_;
	private final Component browerUI_;
	private boolean browserFocus_ = true;

	/**
	 * To display a simple browser window, it suffices completely to create an
	 * instance of the class CefBrowser and to assign its UI component to your
	 * application (e.g. to your content pane). But to be more verbose, this CTOR
	 * keeps an instance of each object on the way to the browser UI.
	 */
	private MainFrame(String startURL, boolean useOSR, boolean isTransparent, String[] args)
			throws UnsupportedPlatformException, CefInitializationException, IOException, InterruptedException {
		// (0) Initialize CEF using the maven loader
		CefAppBuilder builder = new CefAppBuilder();
		builder.setSkipInstallation(true);
		// windowless_rendering_enabled must be set to false if not wanted.
		builder.getCefSettings().windowless_rendering_enabled = useOSR;
		// USE builder.setAppHandler INSTEAD OF CefApp.addAppHandler!
		// Fixes compatibility issues with MacOSX
		builder.setAppHandler(new MavenCefAppHandlerAdapter() {
			@Override
			public void stateHasChanged(org.cef.CefApp.CefAppState state) {
				// Shutdown the app if the native CEF part is terminated
				if (state == CefAppState.TERMINATED)
					System.exit(0);
			}
		});

		if (args.length > 0) {
			builder.addJcefArgs(args);
		}

		// (1) The entry point to JCEF is always the class CefApp. There is only one
		// instance per application and therefore you have to call the method
		// "getInstance()" instead of a CTOR.
		//
		// CefApp is responsible for the global CEF context. It loads all
		// required native libraries, initializes CEF accordingly, starts a
		// background task to handle CEF's message loop and takes care of
		// shutting down CEF after disposing it.
		//
		// WHEN WORKING WITH MAVEN: Use the builder.build() method to
		// build the CefApp on first run and fetch the instance on all consecutive
		// runs. This method is thread-safe and will always return a valid app
		// instance.
		cefApp_ = builder.build();

		// (2) JCEF can handle one to many browser instances simultaneous. These
		// browser instances are logically grouped together by an instance of
		// the class CefClient. In your application you can create one to many
		// instances of CefClient with one to many CefBrowser instances per
		// client. To get an instance of CefClient you have to use the method
		// "createClient()" of your CefApp instance. Calling an CTOR of
		// CefClient is not supported.
		//
		// CefClient is a connector to all possible events which come from the
		// CefBrowser instances. Those events could be simple things like the
		// change of the browser title or more complex ones like context menu
		// events. By assigning handlers to CefClient you can control the
		// behavior of the browser. See tests.detailed.MainFrame for an example
		// of how to use these handlers.
		client_ = cefApp_.createClient();

		// (3) Create a simple message router to receive messages from CEF.
		CefMessageRouter msgRouter = CefMessageRouter.create();

		// 添加消息路由处理器处理JavaScript请求
		msgRouter.addHandler(new CefMessageRouterHandlerAdapter() {
			@Override
			public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent,
					CefQueryCallback callback) {

				System.out.println("收到JavaScript请求: " + request);

				if (request.equals("currentTime")) {
					// 方法1: 获取Java当前时间
					String currentTime = java.time.LocalDateTime.now().toString();
					callback.success(currentTime);
					return true;
				} else if (request.startsWith("add:")) {
					// 方法2: Java加法计算
					try {
						String[] parts = request.substring(4).split(",");
						int a = Integer.parseInt(parts[0].trim());
						int b = Integer.parseInt(parts[1].trim());
						int result = a + b;
						callback.success(String.valueOf(result));
						return true;
					} catch (Exception e) {
						callback.failure(-1, "计算错误: " + e.getMessage());
						return true;
					}
				} else if (request.startsWith("showMessage:")) {
					// 方法3: 显示Java消息框
					String message = request.substring(12);
					SwingUtilities.invokeLater(() -> {
						JOptionPane.showMessageDialog(MainFrame.this, message, "来自Java的消息",
								JOptionPane.INFORMATION_MESSAGE);
					});
					callback.success("消息已显示");
					return true;
				} else {
					// 未知请求
					callback.failure(-1, "未知请求: " + request);
					return true;
				}
			}
		}, true);

		client_.addMessageRouter(msgRouter);

		// (4) One CefBrowser instance is responsible to control what you'll see on
		// the UI component of the instance. It can be displayed off-screen
		// rendered or windowed rendered. To get an instance of CefBrowser you
		// have to call the method "createBrowser()" of your CefClient
		// instances.
		//
		// CefBrowser has methods like "goBack()", "goForward()", "loadURL()",
		// and many more which are used to control the behavior of the displayed
		// content. The UI is held within a UI-Compontent which can be accessed
		// by calling the method "getUIComponent()" on the instance of CefBrowser.
		// The UI component is inherited from a java.awt.Component and therefore
		// it can be embedded into any AWT UI.
		browser_ = client_.createBrowser(startURL, useOSR, isTransparent);
		browerUI_ = browser_.getUIComponent();

		// (5) For this minimal browser, we need only a text field to enter an URL
		// we want to navigate to and a CefBrowser window to display the content
		// of the URL. To respond to the input of the user, we're registering an
		// anonymous ActionListener. This listener is performed each time the
		// user presses the "ENTER" key within the address field.
		// If this happens, the entered value is passed to the CefBrowser
		// instance to be loaded as URL.
		address_ = new JTextField(startURL, 100);
		address_.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				browser_.loadURL(address_.getText());
			}
		});

		// 创建Java控制面板，用于调用JavaScript方法
		JPanel controlPanel = new JPanel(new FlowLayout());

		JButton jsTimeBtn = new JButton("调用JS显示时间");
		jsTimeBtn.addActionListener(e -> {
			String script = "document.getElementById('result').innerHTML = 'Java调用JS: ' + new Date().toLocaleString();";
			browser_.executeJavaScript(script, null, 0);
		});

		JButton jsAlertBtn = new JButton("调用JS显示消息");
		jsAlertBtn.addActionListener(e -> {
			String script = "document.getElementById('result').innerHTML = 'Java调用JavaScript显示的消息: 你好！';"
					+ "document.getElementById('result').style.backgroundColor = '#fff3cd';"
					+ "document.getElementById('result').style.border = '2px solid #ffeaa7';";
			browser_.executeJavaScript(script, null, 0);
		});

		JButton jsCalcBtn = new JButton("调用JS计算");
		jsCalcBtn.addActionListener(e -> {
			String script = "var result = 15 * 8; document.getElementById('result').innerHTML = 'Java调用JS计算 15 × 8 = ' + result;";
			browser_.executeJavaScript(script, null, 0);
		});

		JButton jsColorBtn = new JButton("改变JS页面颜色");
		jsColorBtn.addActionListener(e -> {
			String script = "document.body.style.backgroundColor = '#e6f3ff'; document.getElementById('result').innerHTML = 'Java已改变页面背景色';";
			browser_.executeJavaScript(script, null, 0);
		});

		JButton jsWelcomeBtn = new JButton("调用JS欢迎方法");
		jsWelcomeBtn.addActionListener(e -> {
			browser_.executeJavaScript("showWelcome();", null, 0);
		});

		JButton jsComplexBtn = new JButton("调用JS复杂计算");
		jsComplexBtn.addActionListener(e -> {
			String script = "var result = complexCalculation(3, 4); document.getElementById('result').innerHTML = 'Java调用JS计算√(3²+4²) = ' + result;";
			browser_.executeJavaScript(script, null, 0);
		});

		JButton jsDynamicBtn = new JButton("调用JS创建内容");
		jsDynamicBtn.addActionListener(e -> {
			browser_.executeJavaScript("createDynamicContent();", null, 0);
		});

		JButton jsClearBtn = new JButton("调用JS清空结果");
		jsClearBtn.addActionListener(e -> {
			browser_.executeJavaScript("clearResult();", null, 0);
		});

		controlPanel.add(jsTimeBtn);
		controlPanel.add(jsAlertBtn);
		controlPanel.add(jsCalcBtn);
		controlPanel.add(jsColorBtn);
		controlPanel.add(jsWelcomeBtn);
		controlPanel.add(jsComplexBtn);
		controlPanel.add(jsDynamicBtn);
		controlPanel.add(jsClearBtn);

		// Update the address field when the browser URL changes.
		client_.addDisplayHandler(new CefDisplayHandlerAdapter() {
			@Override
			public void onAddressChange(CefBrowser browser, CefFrame frame, String url) {
				address_.setText(url);
			}
		});

		// Clear focus from the browser when the address field gains focus.
		address_.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				if (!browserFocus_)
					return;
				browserFocus_ = false;
				KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
				address_.requestFocus();
			}
		});

		// Clear focus from the address field when the browser gains focus.
		client_.addFocusHandler(new CefFocusHandlerAdapter() {
			@Override
			public void onGotFocus(CefBrowser browser) {
				if (browserFocus_)
					return;
				browserFocus_ = true;
				KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
				browser.setFocus(true);
			}

			@Override
			public void onTakeFocus(CefBrowser browser, boolean next) {
				browserFocus_ = false;
			}
		});

		// (6) All UI components are assigned to the default content pane of this
		// JFrame and afterwards the frame is made visible to the user.
		JPanel topPanel = new JPanel(new BorderLayout());
		topPanel.add(address_, BorderLayout.CENTER);
		topPanel.add(controlPanel, BorderLayout.SOUTH);

		getContentPane().add(topPanel, BorderLayout.NORTH);
		getContentPane().add(browerUI_, BorderLayout.CENTER);
		pack();
		setSize(800, 650);
		setVisible(true);

		// (7) To take care of shutting down CEF accordingly, it's important to call
		// the method "dispose()" of the CefApp instance if the Java
		// application will be closed. Otherwise you'll get asserts from CEF.
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				CefApp.getInstance().dispose();
				dispose();
			}
		});
	}

	public static void main(String[] args)
			throws UnsupportedPlatformException, CefInitializationException, IOException, InterruptedException {
		// Print some info for the test reports. You can ignore this.
		TestReportGenerator.print(args);

		// The simple example application is created as anonymous class and points
		// to a local HTML file as the very first loaded page. Windowed rendering mode
		// is used by
		// default. If you want to test OSR mode set |useOsr| to true and recompile.
		boolean useOsr = false;
		// 加载自建的HTML文件
		String startURL = "file:///" + System.getProperty("user.dir") + "/index.html";
		new MainFrame(startURL, useOsr, false, args);
	}
}
