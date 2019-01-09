package com.sheinh.ytsplit

import javafx.application.Platform
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.FXCollections
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.control.cell.TextFieldTableCell
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode
import javafx.scene.input.TransferMode
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.util.Callback
import javafx.util.converter.DefaultStringConverter
import javafx.util.converter.IntegerStringConverter
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.concurrent.Executors
import java.util.regex.Pattern

class Controller(val stage : Stage){

    //FXML UI Components
    @FXML
    private lateinit var mainVBox : VBox
    @FXML
    private lateinit var urlField : TextField
    @FXML
    private lateinit var outputFolderField : TextField
    @FXML
    private lateinit var outputFolderChooserButton : Button
    @FXML
    private lateinit var downloadButton : Button
    @FXML
    private lateinit var descriptionBox : TextArea
    @FXML
    private lateinit var getDescriptionButton : Button
    @FXML
    private lateinit var regexButton : Button
    @FXML
    private lateinit var backButton : Button
    @FXML
    private lateinit var regexField : AutoCompleteTextField
    @FXML
    private lateinit var albumField : TextField
    @FXML
    private lateinit var bitrateField : TextField
    @FXML
    private lateinit var formatComboBox : ComboBox<String>
    @FXML
    private lateinit var songsTable : TableView<Song>
    @FXML
    private lateinit var albumArt : ImageView
    @FXML
    private lateinit var secondPaneBottomBar : HBox
    @FXML
    private lateinit var secondPaneVBox: VBox

    private var progressBar = ProgressBar()

    internal lateinit var firstPane : Parent
    internal lateinit var secondPane : Parent

    private val youtubeDL = YouTubeDL()
    private var songs = ArrayList<Song>()
    private val outputFolderChooser = DirectoryChooser()
    private val thread = Executors.newSingleThreadExecutor()

    private val album get() = albumField.text
    private val outputDirectory get() = Path.of(outputFolderField.text)
    private val codec get() = when(formatComboBox.selectionModel.selectedIndex) {
        0 -> "m4a"
        1 -> "m4a"
        else -> "m4a"
    }

    init{
        progressBar.maxHeight = Double.MAX_VALUE
        progressBar.maxWidth = Double.MAX_VALUE
        progressBar.progress = 0.0
    }
    @FXML
    private fun initialize(){
    }
    class MyDialog : Stage(){
        val taskDone = SimpleBooleanProperty(false)
        init {
            setOnCloseRequest { if(!taskDone.value) it.consume() }
        }
    }
    fun showLoadingDialog(task: () -> Unit){
        val dialog = MyDialog()
        dialog.initOwner(stage)
        dialog.initModality(Modality.APPLICATION_MODAL)
        val node = FXMLLoader(javaClass.getResource("/Setup.fxml")).load<Any>()
        dialog.scene = Scene(node as Parent)
        dialog.show()
        thread.submit{
            try {
                task()
                dialog.taskDone.value = true
                Platform.runLater { dialog.close() }
            }finally{
            }
        }
    }
    internal fun firstPaneInit(){
        getDescriptionButton.setOnAction { handleDescriptionButton() }
        regexButton.setOnAction { handleRegexButton() }
        urlField.setOnKeyPressed {
            if(it.code == KeyCode.ENTER){
                getDescriptionButton.fire()
            }
        }
        regexField.entries.addAll(listOf("{ARTIST}", "{TIME}", "{SONG}"))
        regexField.setOnKeyPressed {
            if(it.code == KeyCode.ENTER){
                regexButton.fire()
            }
        }
    }

    fun setupAutoComplete(){
        val suggestions = TreeSet<String>()
        val popup = ContextMenu()
        regexField.textProperty().addListener{_, _, new ->
            if(regexField.text.isEmpty())
                popup.hide()
            else{
                val matcher = Pattern.compile("\\s(\\S)+\$").matcher(regexField.text)
                val lastword = matcher.group(1)
                val results = suggestions.subSet(regexField.text,regexField.text + Character.MAX_VALUE)
                if(!results.isEmpty()){

                }
            }
        }
    }
    internal fun secondPaneInit() {
        if(!Dependancies.checkDependancies() && isWindows){
            showLoadingDialog {
                Dependancies.getDependancies()
            }
        }
        outputFolderChooserButton.setOnAction{ handleFolderChoose() }
        downloadButton.setOnAction { handleDownloadButton() }
        outputFolderField.textProperty().addListener { _, _, _ -> updateDownloadButton() }
        bitrateField.textProperty().addListener { _, _, _ -> updateDownloadButton() }
        formatComboBox.selectionModelProperty().addListener{_, _,_ -> updateDownloadButton() }
        formatComboBox.selectionModel.selectedItemProperty().addListener{ _, _, _ -> updateDownloadButton() }
        backButton.setOnAction { firstPaneSwitch() }
        youtubeDL.albumArtProperty.addListener{_,_,new ->
            if(new == null)
                return@addListener
            val image = Image(new.toUri().toString(),200.0,200.0,false,true)
            albumArt.image = image
        }
        albumArt.setOnDragOver {
            if (it.gestureSource != albumArt
                    && it.getDragboard().hasFiles()) {
                /* allow for both copying and moving, whatever user chooses */
                it.acceptTransferModes(*TransferMode.COPY_OR_MOVE);
            }
            it.consume();
        }
        albumArt.setOnDragDropped {
            val db = it.dragboard
                var success = false
                if (db.hasFiles()) {
                    if(db.files.size == 1){
                        val fil = db.files[0]
                        try{
                            youtubeDL.albumArt = fil.toPath()
                        }
                        catch(e : Exception){ }
                    }
                }
                it.isDropCompleted = success;

                it.consume()
        }
        albumArt.setOnMouseClicked { if(it.isPrimaryButtonDown) handleAlbumArtChange() }
        val menu = ContextMenu()
        val items = arrayOf(MenuItem("Set Album Art"),MenuItem("Restore Default Art"))
        items[0].setOnAction { handleAlbumArtChange() }
        items[1].setOnAction { youtubeDL.setDefaultArt() }
        menu.items.addAll(items)
        albumArt.onContextMenuRequested = EventHandler{
            menu.show(albumArt,it.screenX,it.screenY)
        }

        songsTable.columns.clear()
        val track = TableColumn<Song,Int>("#")
        track.cellFactory = Callback<TableColumn<Song, Int>, TableCell<Song, Int>> { TextFieldTableCell<Song, Int>(IntegerStringConverter()) }
        track.cellValueFactory =  PropertyValueFactory<Song, Int>("trackNo")
        track.setOnEditCommit{ it.rowValue.trackNo = it.newValue}
        val song = TableColumn<Song,String>("Song")
        song.cellValueFactory = PropertyValueFactory<Song, String>("song")
        song.cellFactory = Callback<TableColumn<Song, String>, TableCell<Song, String>> { TextFieldTableCell<Song, String>(DefaultStringConverter()) }
        song.setOnEditCommit{ it.rowValue.song = it.newValue}
        val artist = TableColumn<Song,String>("Artist")
        artist.cellValueFactory = PropertyValueFactory<Song, String>("artist")
        artist.cellFactory = Callback<TableColumn<Song, String>, TableCell<Song, String>> {
            val cell = TextFieldTableCell<Song, String>(DefaultStringConverter())
            val item1 = MenuItem("Set artist for all")
            item1.setOnAction{songs.forEach{ it.artist = cell.text}}
            cell.contextMenu = ContextMenu()
            cell.contextMenu.items.add(item1)
            songsTable.items = FXCollections.observableList(songs)
            cell
        }
        song.minWidth = 170.0
        artist.minWidth = 170.0
        artist.setOnEditCommit{ it.rowValue.artist = it.newValue}
        songsTable.editableProperty().value = true;
        songsTable.columns.addAll(track,song,artist)
    }

    private fun handleAlbumArtChange() {
        val fc = FileChooser()
        fc.extensionFilters.add(FileChooser.ExtensionFilter("Image Files","*.jpg","*.jpeg","*.png","*.bmp","*.tiff","*.gif"))
        fc.title = "Select Album Art"
        val newImg = fc.showOpenDialog(stage)
        if(newImg == null)
            return
        youtubeDL.albumArt = newImg.toPath()
    }

    internal fun secondPaneSwitch(){
        songsTable.items = FXCollections.observableList(songs)
        albumField.text = youtubeDL.getProperty("title")
        stage.scene.root = secondPane
        stage.sizeToScene()
    }
    internal fun firstPaneSwitch(){
        urlField.text = youtubeDL.url
        songsTable.items.clear()
        albumField.text = ""
        bitrateField.text = ""
        formatComboBox.selectionModel.clearSelection()
        stage.scene.root = firstPane
        stage.sizeToScene()
    }
    private var progressBarShown = false
    set(value){
        if(field == value)
            return
        if(value){
            val index = secondPaneVBox.children.indexOf(secondPaneBottomBar)
            secondPaneVBox.children.removeAt(index)
            secondPaneVBox.children.add(progressBar)
        }
        else{
            val index = secondPaneVBox.children.indexOf(progressBar)
            secondPaneVBox.children.removeAt(index)
            secondPaneVBox.children.add(secondPaneBottomBar)
        }
        field = value
    }
    private fun handleFolderChoose(){
        var folder : File? = outputFolderChooser.showDialog(stage)
        outputFolderField.text = folder?.absolutePath
    }
    private fun handleDownloadButton(){
        try {
            outputFolderField.disableProperty().value = true
            songsTable.disableProperty().value = true
            albumField.disableProperty().value = true
            bitrateField.disableProperty().value = true
            formatComboBox.disableProperty().value = true
            outputFolderChooserButton.disableProperty().value = true
            songs.forEach{ it.album = album }
            val numTasks = (songs.size + 1).toDouble()
            var tasksCompleted = 0
            val addProgress = {Platform.runLater{
                tasksCompleted++
                progressBar.progress = tasksCompleted/numTasks
            }}
            progressBarShown = true
            var directory = outputDirectory.resolve(album)
            if(!Files.exists(directory))
                Files.createDirectories(directory)
            thread.submit{
                try {
                    youtubeDL.download()
                    Platform.runLater{ addProgress() }
                    youtubeDL.save(directory, codec, bitrateField.text.toInt(), songs, addProgress)
                    progressBarShown = false
                }finally{
                    outputFolderField.disableProperty().value = false
                    songsTable.disableProperty().value = false
                    albumField.disableProperty().value = false
                    bitrateField.disableProperty().value = false
                    formatComboBox.disableProperty().value = false
                    outputFolderChooserButton.disableProperty().value = false
                    Platform.runLater{ progressBarShown = false }
                }
            }
        }
        catch(e : FileNotFoundException){
            outputFolderField.disableProperty().value = false
            songsTable.disableProperty().value = false
            albumField.disableProperty().value = false
            bitrateField.disableProperty().value = false
            formatComboBox.disableProperty().value = false
            outputFolderChooserButton.disableProperty().value = false
            progressBarShown = false
        }
    }
    private fun updateDownloadButton(){
        if(formatComboBox.selectionModel.selectedItem == null){
            downloadButton.disableProperty().value = true
            return
        }
        try{
            val int = bitrateField.text.toInt()
        } catch(e : NumberFormatException) {
            downloadButton.disableProperty().value = true
            return
        }
        val file = File(outputFolderField.text)
        if(file.exists() && file.isDirectory){
            downloadButton.disableProperty().value = false
            return
        }
        else{
            downloadButton.disableProperty().value = true
        }
    }
    private fun handleDescriptionButton(){
        if(getDescriptionButton.graphic != null)
            return
        regexField.requestFocus()
        descriptionBox.disableProperty().value = true
        regexButton.disableProperty().value = true
        val oldtext = getDescriptionButton.text
        getDescriptionButton.text = ""
        val indicator = ProgressIndicator()
        indicator.maxHeight = 20.0
        indicator.maxWidth = 20.0
        getDescriptionButton.graphic = indicator
        thread.submit{
            try {
                youtubeDL.url = urlField.text
                youtubeDL.loadJsonData()
                val description = youtubeDL.getProperty("description")
                descriptionBox.text = description
                regexButton.disableProperty().value = false
            }
            finally{
                descriptionBox.disableProperty().value = false
                regexButton.disableProperty().value = false
                getDescriptionButton.disableProperty().value = false
                Platform.runLater {
                    getDescriptionButton.text = oldtext
                    getDescriptionButton.graphic = null
                }
            }
        }
        thread.submit{
            youtubeDL.fetchAlbumArt()
        }
    }
    private fun handleRegexButton(){
            try {
                val pattern = RegexStuff.inputToRegex(regexField.text)
                val matcher = pattern.matcher(descriptionBox.text + System.lineSeparator())
                songs = RegexStuff.matchSongs(matcher)
                secondPaneSwitch()
            }
            catch(e : Exception){
                e.printStackTrace()
                return
            }
    }
}