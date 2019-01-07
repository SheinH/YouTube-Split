package com.sheinh.ytsplit

import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.control.*
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.control.cell.TextFieldTableCell
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import javafx.stage.DirectoryChooser
import javafx.stage.Stage
import javafx.util.Callback
import javafx.util.StringConverter
import javafx.util.converter.DefaultStringConverter
import javafx.util.converter.IntegerStringConverter
import javafx.util.converter.NumberStringConverter
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Executors

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
    private lateinit var regexField : TextField
    @FXML
    private lateinit var albumField : TextField
    @FXML
    private lateinit var bitrateField : TextField
    @FXML
    private lateinit var formatComboBox : ComboBox<String>
    @FXML
    private lateinit var songsTable : TableView<Song>

    internal lateinit var firstPane : Parent
    internal lateinit var secondPane : Parent

    private val youtubeDL = YouTubeDL()
    private var songs = ArrayList<Song>()
    private val outputFolderChooser = DirectoryChooser()
    private val thread = Executors.newSingleThreadExecutor()

    private var tableViewShown : Boolean = false
    set(value){
        if(value == tableViewShown)
            return
        field = value
        updateDownloadButton()
        if(value){
            getDescriptionButton.disableProperty().value = true;
            urlField.editableProperty().value = false;
            regexField.disableProperty().value = true;
            mainVBox.children.removeAt(5)
            mainVBox.children.add(5,songsTable)
            regexButton.text="⬅"
            stage.sizeToScene()
        }
        else{
            getDescriptionButton.disableProperty().value = false;
            urlField.editableProperty().value = true;
            regexField.disableProperty().value = false;
            val index = mainVBox.children.indexOf(songsTable)
            mainVBox.children.remove(songsTable)
            mainVBox.children.add(index,descriptionBox)
            regexButton.text="➡"
            stage.sizeToScene()
        }
    }

    private val album get() = albumField.text
    private val outputDirectory get() = Path.of(outputFolderField.text)
    private val codec get() = when(formatComboBox.selectionModel.selectedIndex) {
        0 -> "opus"
        1 -> "m4a"
        2 -> "mp3"
        else -> "opus"
    }

    init{
    }
    private fun loadSecondPane() {
        stage.scene.root = secondPane
        stage.sizeToScene()
        secondPaneInit()
    }
    @FXML
    private fun initialize(){
    }
    internal fun firstPaneInit(){
        getDescriptionButton.setOnAction { handleDescriptionButton() }
        regexButton.setOnAction { handleRegexButton() }
    }
    internal fun secondPaneInit() {
        albumField.text = youtubeDL.getProperty("title")
        val enc = youtubeDL.acodec
        bitrateField.text = youtubeDL.getProperty("abr")
        formatComboBox.selectionModel.select( if(enc == "m4a") 1 else 0 )
        outputFolderChooserButton.setOnAction{ handleFolderChoose() }
        downloadButton.setOnAction { handleDownloadButton() }
        outputFolderField.textProperty().addListener { _, _, _ -> updateDownloadButton() }
        bitrateField.textProperty().addListener { _, _, _ -> updateDownloadButton() }
        formatComboBox.selectionModel.selectedItemProperty().addListener{ _, _, _ -> updateDownloadButton() }
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
    private fun handleFolderChoose(){
        var folder : File? = outputFolderChooser.showDialog(stage)
        outputFolderField.text = folder?.absolutePath
    }
    private fun handleDownloadButton(){
        try {
            songs.forEach{ it.album = album }
            outputFolderField.disableProperty().value = true
            youtubeDL.download()
            var directory = outputDirectory.resolve(album)
            if(!Files.exists(directory))
                Files.createDirectories(directory)
            youtubeDL.save(directory,codec,bitrateField.text.toInt(),songs)
        }
        catch(e : FileNotFoundException){}
        finally{
            outputFolderField.disableProperty().value = false
        }
    }
    private fun updateDownloadButton(){
        if(formatComboBox.selectionModel.selectedItem.isEmpty()){
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
        thread.submit{
            youtubeDL.url = urlField.text
            youtubeDL.loadJsonData()
            val description = youtubeDL.getProperty("description")
            descriptionBox.text = description
        }
    }
    private fun handleRegexButton(){
        if(!tableViewShown) {
            try {
                val pattern = RegexStuff.inputToRegex(regexField.text)
                val matcher = pattern.matcher(descriptionBox.text + System.lineSeparator())
                songs = RegexStuff.matchSongs(matcher)
                songsTable.items = FXCollections.observableList(songs)
                loadSecondPane()
            }
            catch(e : Exception){
                e.printStackTrace()
                return
            }
        }
    }
}