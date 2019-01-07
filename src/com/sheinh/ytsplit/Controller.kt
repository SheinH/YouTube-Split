package com.sheinh.ytsplit

import javafx.beans.property.SimpleIntegerProperty
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.control.cell.TextFieldTableCell
import javafx.scene.layout.VBox
import javafx.stage.DirectoryChooser
import javafx.stage.Stage
import javafx.util.Callback
import javafx.util.StringConverter
import javafx.util.converter.DefaultStringConverter
import java.io.File
import java.io.FileNotFoundException
import java.lang.NumberFormatException
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

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

    private val youtubeDL : SplitIO()
    private val songsTable = TableView<Song>()
    private var songs = ArrayList<Song>()
    set(value){
        if(value == songs)
            return;
        downloadButton.disableProperty().value = value == null;
        field = value
    }
    private val outputFolderChooser = DirectoryChooser()

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

    init{
        songsTable.prefWidth = 500.0
        songsTable.prefHeight = 300.0
        songsTable.isEditable = true
        val track = TableColumn<Song,Int>("#")
        track.cellFactory = Callback<TableColumn<Song, Int>, TableCell<Song, Int>> { TextFieldTableCell<Song, Int>() }
        track.cellValueFactory =  PropertyValueFactory<Song, Int>("trackNo")
        track.setOnEditCommit{ it.rowValue.trackNo = it.newValue}
        val song = TableColumn<Song,String>("Song")
        song.cellValueFactory = PropertyValueFactory<Song, String>("song")
        song.cellFactory = Callback<TableColumn<Song, String>, TableCell<Song, String>> { TextFieldTableCell<Song, String>() }
        song.setOnEditCommit{ it.rowValue.song = it.newValue}
        val artist = TableColumn<Song,String>("Artist")
        artist.cellValueFactory = PropertyValueFactory<Song, String>("artist")
        artist.cellFactory = Callback<TableColumn<Song, String>, TableCell<Song, String>> { TextFieldTableCell<Song, String>() }
        artist.setOnEditCommit{ it.rowValue.artist = it.newValue}
        songsTable.editableProperty().value = true;
        songsTable.columns.addAll(track,song,artist);
    }
    @FXML
    private fun initialize(){
        outputFolderChooserButton.setOnAction{ handleFolderChoose() }
        downloadButton.setOnAction { handleDownloadButton() }
        getDescriptionButton.setOnAction { handleDescriptionButton() }
        regexButton.setOnAction { handleRegexButton() }
        outputFolderField.textProperty().addListener { _, _, _ -> updateDownloadButton() }
        bitrateField.textProperty().addListener { _, _, _ -> updateDownloadButton() }
        formatComboBox.selectionModel.selectedItemProperty().addListener{ _, _, _ -> updateDownloadButton() }

    }
    private fun handleFolderChoose(){
        var folder : File? = outputFolderChooser.showDialog(stage)
        outputFolderField.text = folder?.absolutePath
    }
    private fun handleDownloadButton(){
        try {
            outputFolderField.disableProperty().value = true
            var output = YouTubeDL.download(File(outputFolderField.text))
            var file = File(outputFolderField.text + File.separator + output)
            val codec = when(formatComboBox.selectionModel.selectedIndex){
                0 -> YouTubeDL.Codec.opus
                1 -> YouTubeDL.Codec.m4a
                2 -> YouTubeDL.Codec.mp3
                else -> YouTubeDL.Codec.opus
            }
            val enc = YouTubeDL.Encoding(bitrateField.text.toInt(),codec)
            YouTubeDL.split(file,songs,enc)
            songs.forEach { YouTubeDL.writeAlbum(it,albumField.text) }
        }
        catch(e : FileNotFoundException){}
        finally{
            outputFolderField.disableProperty().value = false
        }
    }
    private fun updateDownloadButton(){
        if(!tableViewShown) {
            downloadButton.disableProperty().value = true
            return
        }
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
        val thread = Thread{
            youtubeDL.url = urlField.text
            youtubeDL.loadJsonData()
            val description = youtubeDL.getProperty("description")
            descriptionBox.text = description
            albumField.text = youtubeDL.getProperty("title")
            val enc = youtubeDL.acodec
            bitrateField.text = youtubeDL.getProperty("abr")
            formatComboBox.selectionModel.select( if(enc == "m4a") 1 else 0 )
        }
        thread.run()
    }
    private fun handleRegexButton(){
        if(!tableViewShown) {
            try {
                val pattern = RegexStuff.inputToRegex(regexField.text)
                val matcher = pattern.matcher(descriptionBox.text + System.lineSeparator())
                songs = RegexStuff.matchSongs(matcher)
                songsTable.populate(songs)
                tableViewShown = !tableViewShown
            }
            catch(e : Exception){
                return
            }
        }
    }
    fun TableView<Song>.populate(list : List<Song>){
        this.items = FXCollections.observableList(list)
    }
}