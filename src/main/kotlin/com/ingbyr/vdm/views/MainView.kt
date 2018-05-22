package com.ingbyr.vdm.views

import com.ingbyr.vdm.controllers.MainController
import com.ingbyr.vdm.events.StopBackgroundTask
import com.ingbyr.vdm.models.DownloadTaskModel
import com.ingbyr.vdm.utils.VDMConfigUtils
import com.ingbyr.vdm.utils.VDMOSUtils
import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXCheckBox
import com.jfoenix.controls.JFXProgressBar
import javafx.geometry.Insets
import javafx.scene.control.*
import javafx.scene.layout.ColumnConstraints
import javafx.scene.layout.GridPane
import javafx.scene.layout.VBox
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tornadofx.*
import java.text.DecimalFormat
import java.util.*


/**
 * TODO add enable debug mode button
 * TODO add FFMPEG and cookie
 */
class MainView : View() {
    init {
        messages = ResourceBundle.getBundle("i18n/MainView")
    }

    private val logger: Logger = LoggerFactory.getLogger(MainView::class.java)
    override val root: VBox by fxml("/fxml/MainView.fxml")
    private val controller: MainController by inject()

    private val btnNew: JFXButton by fxid()
    private val btnStart: JFXButton by fxid()
    private val btnStop: JFXButton by fxid()
    private val btnDelete: JFXButton by fxid()
    private val btnOpenFile: JFXButton by fxid()
    private val btnSearch: JFXButton by fxid()
    private val btnPreferences: JFXButton by fxid()
    private val btnMenu: JFXButton by fxid()
    private val contextMenu: ContextMenu = ContextMenu()

    private var menuNew: MenuItem
    private var menuOpenDir: MenuItem
    private var menuStartAllTask: MenuItem
    private var menuStopAllTask: MenuItem
    private var menuPreferences: MenuItem
    private var menuAbout: MenuItem
    private var menuQuit: MenuItem
    private var menuDonate: MenuItem
    private lateinit var labelStatus: Label

    private var selectedTaskModel: DownloadTaskModel? = null
    private lateinit var downloadTaskTableView: TableView<DownloadTaskModel>

    private val cu = VDMConfigUtils(app.config)

    init {
        root += anchorpane {
            fitToParentSize()
            padding = Insets(10.0)
            downloadTaskTableView = tableview(controller.downloadTaskModelList) {
                fitToParentSize()
                columnResizePolicy = SmartResize.POLICY
                column("", DownloadTaskModel::checkedProperty).cellFormat {
                    val cb = JFXCheckBox("")
                    cb.isSelected = it
                    graphic = cb
                }
                column(messages["ui.title"], DownloadTaskModel::titleProperty).pctWidth(40)
                column(messages["ui.size"], DownloadTaskModel::sizeProperty)
                column(messages["ui.status"], DownloadTaskModel::statusProperty).cellFormat {
                    labelStatus = Label(it)
                    graphic = labelStatus
                }
                column(messages["ui.progress"], DownloadTaskModel::progressProperty).pctWidth(20).cellFormat {
                    val progressFormat = DecimalFormat("#.##")
                    val progressPane = GridPane()
                    val progressBar = JFXProgressBar(it.toDouble())
                    val progressLabel = Label(progressFormat.format(it.toDouble() * 100) + "%")
                    progressPane.useMaxSize = true
                    progressPane.add(progressBar, 0, 0)
                    progressPane.add(progressLabel, 1, 0)
                    val columnBar = ColumnConstraints()
                    columnBar.percentWidth = 75.0
                    val columnLabel = ColumnConstraints()
                    columnLabel.percentWidth = 25.0
                    progressPane.columnConstraints.addAll(columnBar, columnLabel)
                    progressPane.hgap = 10.0
                    progressBar.useMaxSize = true
                    progressLabel.useMaxWidth = true
                    graphic = progressPane
                }
                column(messages["ui.createdAt"], DownloadTaskModel::createdAtProperty)
            }
        }

        // init context menu
        menuNew = MenuItem(messages["ui.new"])
        menuOpenDir = MenuItem(messages["ui.openDirectory"])
        menuStartAllTask = MenuItem(messages["ui.startAllTask"])
        menuStopAllTask = MenuItem(messages["ui.stopAllTask"])
        menuPreferences = MenuItem(messages["ui.preferences"])
        menuAbout = MenuItem(messages["ui.about"])
        menuQuit = MenuItem(messages["ui.quit"])
        menuDonate = MenuItem(messages["ui.donate"])
        contextMenu.items.addAll(menuNew, menuOpenDir, menuStartAllTask, menuStopAllTask, SeparatorMenuItem(), menuPreferences, menuAbout, menuDonate, SeparatorMenuItem(), menuQuit)
        loadVDMConfig()
        initListeners()
        controller.loadTaskFromDB()
    }

    private fun loadVDMConfig() {
        // create the config file when first time use VDM
        val firstTimeUse = cu.safeLoad(VDMConfigUtils.FIRST_TIME_USE, "true").toBoolean()
        if (firstTimeUse) {
            // init config file
            find(PreferencesView::class).openWindow()?.hide()
            cu.update(VDMConfigUtils.FIRST_TIME_USE, "false")
            cu.saveToConfigFile()
        }
    }

    private fun initListeners() {
        // task list view
        downloadTaskTableView.selectionModel.selectedItemProperty().addListener { _, _, selectedItem ->
            selectedTaskModel = selectedItem
        }

        // shortcut buttons
        // start task
        btnStart.setOnMouseClicked {
            selectedTaskModel?.let {
                controller.startDownloadTask(it)
            }
        }
        // preferences view
        btnPreferences.setOnMouseClicked {
            find(PreferencesView::class).openWindow()
        }
        // create task
        btnNew.setOnMouseClicked {
            find(CreateDownloadTaskView::class).openWindow()
        }
        // delete task
        btnDelete.setOnMouseClicked {
            selectedTaskModel?.run {
                controller.deleteTask(this)
            }
        }
        // stop task
        btnStop.setOnMouseClicked {
            selectedTaskModel?.run {
                fire(StopBackgroundTask(this, false))
            }
        }
        // open dir
        btnOpenFile.setOnMouseClicked {
            if (selectedTaskModel != null) {
                VDMOSUtils.openDir(selectedTaskModel!!.vdmConfig.storagePath)
            } else {
                VDMOSUtils.openDir(cu.load(VDMConfigUtils.STORAGE_PATH))
            }
        }
        // search
        btnSearch.setOnMouseClicked {
            // TODO search task
        }

        // menus
        btnMenu.setOnMouseClicked {
            contextMenu.show(primaryStage, it.screenX, it.screenY)
        }
        menuNew.action {
            find(CreateDownloadTaskView::class).openWindow()
        }
        menuOpenDir.action {
            if (selectedTaskModel != null) {
                VDMOSUtils.openDir(selectedTaskModel!!.vdmConfig.storagePath)
            } else {
                VDMOSUtils.openDir(cu.load(VDMConfigUtils.STORAGE_PATH))
            }
        }
        menuStartAllTask.action {
            controller.startAllDownloadTask()
        }
        menuStopAllTask.action {
            fire(StopBackgroundTask(stopAll = true))
        }
        menuPreferences.action {
            find(PreferencesView::class).openWindow()
        }
        menuAbout.action {
            // TODO about view
        }
        menuQuit.action {
            this.close()
        }
        menuDonate.action {
            openInternalWindow(DonationView())
        }
    }

    override fun onUndock() {
        super.onUndock()
        controller.clear()
        selectedTaskModel = null
    }
}