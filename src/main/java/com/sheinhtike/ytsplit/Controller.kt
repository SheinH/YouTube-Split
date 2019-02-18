package com.sheinhtike.ytsplit

import javafx.application.Platform
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.FXCollections
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.geometry.Insets
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.control.cell.TextFieldTableCell
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import javafx.scene.input.TransferMode
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.text.Font
import javafx.stage.*
import javafx.util.Callback
import javafx.util.converter.DefaultStringConverter
import javafx.util.converter.IntegerStringConverter
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files
import java.util.*

class Controller(private val stage: Stage) {

	//FXML UI Components
	@FXML
	private lateinit var urlField: TextField
	@FXML
	private lateinit var outputFolderField: TextField
	@FXML
	private lateinit var outputFolderChooserButton: Button
	@FXML
	private lateinit var downloadButton: Button
	@FXML
	private lateinit var descriptionBox: TextArea
	@FXML
	private lateinit var getDescriptionButton: Button
	@FXML
	private lateinit var regexButton: Button
	@FXML
	private lateinit var backButton: Button
	@FXML
	private lateinit var regexField: AutoCompleteTextField
	@FXML
	private lateinit var albumField: TextField
	@FXML
	private lateinit var bitrateField: TextField
	@FXML
	private lateinit var formatComboBox: ComboBox<String>
	@FXML
	private lateinit var songsTable: TableView<Song>
	@FXML
	private lateinit var albumArt: ImageView
	@FXML
	private lateinit var secondPaneBottomBar: HBox
	@FXML
	private lateinit var secondPaneVBox: VBox
	@FXML
	private lateinit var mainVBox: VBox
	@FXML
	private lateinit var updateMenuItem: MenuItem

	private var progressBar = ProgressBar()

	internal lateinit var firstPane: Parent
	internal lateinit var secondPane: Parent

	private val youtubeDL = YouTubeDL()
	private var songs = ArrayList<Song>()
	private val outputFolderChooser = DirectoryChooser()

	private val album get() = albumField.text
	private val outputDirectory get() = File(outputFolderField.text).toPath()
	private val codec
		get() = when (formatComboBox.selectionModel.selectedIndex) {
			0    -> "m4a"
			1    -> "mp3"
			else -> "m4a"
		}

	private var progressBarShown = false
		set(value) {
			progressBar.progress = 0.0
			if (field == value) return
			if (value) {
				VBox.setMargin(progressBar, Insets(12.0, 0.0, 12.0, 0.0))
				secondPaneVBox.children.apply {
					remove(secondPaneBottomBar)
					add(progressBar)
				}
			} else {
				secondPaneVBox.children.apply {
					remove(progressBar)
					add(secondPaneBottomBar)
				}
			}
			field = value
		}

	init {
		progressBar.run {
			prefHeight = 5.0
			prefWidth = Double.MAX_VALUE
			progress = 0.0
		}
	}

	class MyDialog : Stage() {
		val taskDone = SimpleBooleanProperty(false)

		init {
			setOnCloseRequest { if (!taskDone.value) it.consume() }
		}
	}

	fun getDependenciesWin() {
		Platform.runLater {
			showLoadingDialog {
				Dependencies.getDependenciesWin()
			}
		}
	}

	private fun showLoadingDialog(task: () -> Unit) {
		val dialog = MyDialog().apply {
			initOwner(stage)
			title = "Setup"
			initModality(Modality.APPLICATION_MODAL)
			initStyle(StageStyle.UNDECORATED)
			val node = FXMLLoader(javaClass.getResource("/Setup.fxml")).load<Any>()
			scene = Scene(node as Parent)
			show()
		}
		GlobalScope.launch {
			try {
				task()
				dialog.taskDone.value = true
				Platform.runLater { dialog.close() }
			} catch (e: Exception) {
				e.printStackTrace(System.out)
				Platform.runLater {
					Alert(Alert.AlertType.ERROR).apply {
						headerText = null
						title = "Error"
						headerText = "Download failed"
						contentText = "Try again Later"
						width = 220.0
						showAndWait()
					}
					dialog.close()
					stage.close()
				}
			}
		}
	}

	internal fun firstPaneInit() {
		mainVBox.setOnKeyPressed {
			if (KeyCodeCombination(KeyCode.L, KeyCombination.SHORTCUT_DOWN).match(it)) {
				urlField.requestFocus()
				urlField.selectAll()
				it.consume()
			}
		}
		updateMenuItem.setOnAction { handleupdateMenuItem() }
		getDescriptionButton.setOnAction { handleDescriptionButton() }
		regexButton.setOnAction { handleRegexButton() }
		urlField.setOnKeyPressed {
			if (it.code == KeyCode.ENTER) {
				getDescriptionButton.fire()
				it.consume()
			}
		}
		regexField.entries.addAll(listOf("{ARTIST}", "{TIME}", "{SONG}"))
		regexField.setOnKeyPressed {
			if (it.code == KeyCode.ENTER) {
				regexButton.fire()
			}
		}
		val fonts = Font.getFamilies()
		val prefFonts = listOf("Menlo", "Consolas", "Lucida Console", "Monaco").filter { fonts.contains(it) }
		if (prefFonts.isNotEmpty()) descriptionBox.font = Font.font(prefFonts[0])
	}

	fun secondPaneInit() {
		fun initAlbumArt() {
			albumArt.setOnDragOver {
				if (it.gestureSource != albumArt && it.dragboard.hasFiles()) {
					/* allow for both copying and moving, whatever user chooses */
					it.acceptTransferModes(*TransferMode.COPY_OR_MOVE)
				}
				it.consume()
			}
			albumArt.setOnDragDropped {
				val db = it.dragboard
				val success = false
				if (db.hasFiles()) {
					if (db.files.size == 1) {
						val fil = db.files[0]
						try {
							youtubeDL.albumArt = fil.toPath()
						} catch (e: Exception) {
						}
					}
				}
				it.isDropCompleted = success

				it.consume()
			}
			albumArt.setOnMouseClicked { if (it.isPrimaryButtonDown) handleAlbumArtChange() }
			val menu = ContextMenu()
			val items = arrayOf(MenuItem("Set Album Art"), MenuItem("Restore Default Art"))
			items[0].setOnAction { handleAlbumArtChange() }
			items[1].setOnAction { youtubeDL.setDefaultArt() }
			menu.items.addAll(items)
			albumArt.onContextMenuRequested = EventHandler {
				menu.show(albumArt, it.screenX, it.screenY)
			}
			youtubeDL.albumArtProperty.addListener { _, _, new ->
				if (new == null) return@addListener
				val image = Image(new.toUri().toString(), 200.0, 200.0, false, true)
				albumArt.image = image
			}
		}

		fun initTable() {
			songsTable.columns.clear()
			val track = TableColumn<Song, Int>("#")
			track.cellFactory = Callback<TableColumn<Song, Int>, TableCell<Song, Int>> {
				TextFieldTableCell<Song, Int>(IntegerStringConverter())
			}
			track.cellValueFactory = PropertyValueFactory<Song, Int>("trackNo")
			track.setOnEditCommit { it.rowValue.trackNo = it.newValue }
			val song = TableColumn<Song, String>("Song")
			song.cellValueFactory = PropertyValueFactory<Song, String>("song")
			song.cellFactory = Callback<TableColumn<Song, String>, TableCell<Song, String>> {
				TextFieldTableCell<Song, String>(DefaultStringConverter())
			}
			song.setOnEditCommit { it.rowValue.song = it.newValue }
			val artist = TableColumn<Song, String>("Artist")
			artist.cellValueFactory = PropertyValueFactory<Song, String>("artist")
			artist.cellFactory = Callback<TableColumn<Song, String>, TableCell<Song, String>> {
				val cell = TextFieldTableCell<Song, String>(DefaultStringConverter())
				val item1 = MenuItem("Set artist for all")
				item1.setOnAction { songs.forEach { it.artist = cell.text } }
				cell.contextMenu = ContextMenu()
				cell.contextMenu.items.add(item1)
				cell
			}
			track.minWidth = 30.0
			track.maxWidth = 30.0
			artist.setOnEditCommit {
				if (songs.none { it.artist.isNotBlank() }) songs.forEach { s -> s.artist = it.newValue }
			}
			songsTable.editableProperty()
					.value = true
			songsTable.columns.addAll(track, song, artist)
		}

		initAlbumArt()
		initTable()

		outputFolderChooserButton.setOnAction { handleFolderChoose() }
		downloadButton.setOnAction { handleDownloadButton() }
		outputFolderField.textProperty()
				.addListener { _, _, _ -> updateDownloadButton() }
		bitrateField.textProperty()
				.addListener { _, _, _ -> updateDownloadButton() }
		formatComboBox.selectionModelProperty()
				.addListener { _, _, _ -> updateDownloadButton() }
		formatComboBox.selectionModel.selectedItemProperty()
				.addListener { _, _, _ -> updateDownloadButton() }
		backButton.setOnAction { firstPaneSwitch() }
	}

	private fun handleupdateMenuItem(){
		Dependencies.upgradeYoutubeDL();
	}

	private fun handleAlbumArtChange() {
		val fc = FileChooser()
		fc.extensionFilters.add(
				FileChooser.ExtensionFilter(
						"Image Files", "*.jpg", "*.jpeg", "*.png", "*.bmp", "*.tiff", "*.gif"
				)
		)
		fc.title = "Select Album Art"
		val newImg = fc.showOpenDialog(stage) ?: return
		youtubeDL.albumArt = newImg.toPath()
	}

	private fun secondPaneSwitch() {
		songsTable.items = FXCollections.observableList(songs)
		albumField.text = youtubeDL.getProperty("title")
		stage.scene.root = secondPane
		stage.sizeToScene()
	}

	private fun firstPaneSwitch() {
		urlField.text = youtubeDL.url
		songsTable.items.clear()
		albumField.text = ""
		bitrateField.text = ""
		formatComboBox.selectionModel.clearSelection()
		stage.scene.root = firstPane
		stage.sizeToScene()
	}

	private fun handleFolderChoose() {
		val folder = outputFolderChooser.showDialog(stage)
		outputFolderField.text = folder?.absolutePath
		bitrateField.requestFocus()
		bitrateField.selectAll()
	}

	private fun handleDownloadButton() {
		try {
			outputFolderField.disableProperty()
					.value = true
			songsTable.disableProperty()
					.value = true
			albumField.disableProperty()
					.value = true
			bitrateField.disableProperty()
					.value = true
			formatComboBox.disableProperty()
					.value = true
			outputFolderChooserButton.disableProperty()
					.value = true
			albumArt.disableProperty()
					.value = true
			songs.forEach { it.album = album }
			val numTasks = (songs.size + 1).toDouble()
			var tasksCompleted = 0
			val addProgress = {
				tasksCompleted++
				Platform.runLater {
					progressBar.progress = tasksCompleted / numTasks
				}
			}
			progressBarShown = true
			val directory = outputDirectory.resolve(sanitizeFilename(album))
			if (!Files.exists(directory)) Files.createDirectories(directory)
			GlobalScope.launch {
				try {
					youtubeDL.save(directory, codec, bitrateField.text.toInt(), songs, addProgress)
					Platform.runLater {
						progressBarShown = false
						val alert = Alert(Alert.AlertType.INFORMATION).apply {
							title = "Save Complete"
							headerText = null
							contentText = "Files saved to $directory"
							buttonTypes.clear()
							buttonTypes.add(ButtonType.CLOSE)
							buttonTypes.add(ButtonType("Open Folder", ButtonBar.ButtonData.YES))
						}

						val out = alert.showAndWait()

						if (out.isPresent) {
							if (out.get().buttonData == ButtonBar.ButtonData.YES) {
								Desktop.getDesktop()
										.open(directory.toFile())
							}
						}
					}
				} finally {
					outputFolderField.disableProperty()
							.value = false
					songsTable.disableProperty()
							.value = false
					albumField.disableProperty()
							.value = false
					bitrateField.disableProperty()
							.value = false
					formatComboBox.disableProperty()
							.value = false
					outputFolderChooserButton.disableProperty()
							.value = false
					albumArt.disableProperty()
							.value = false
					Platform.runLater { progressBarShown = false }
				}
			}
		} catch (e: FileNotFoundException) {
			outputFolderField.disableProperty()
					.value = false
			songsTable.disableProperty()
					.value = false
			albumField.disableProperty()
					.value = false
			bitrateField.disableProperty()
					.value = false
			formatComboBox.disableProperty()
					.value = false
			outputFolderChooserButton.disableProperty()
					.value = false
			progressBarShown = false
			albumArt.disableProperty()
					.value = false
		}
	}

	@Suppress("UNUSED_VARIABLE")
	private fun updateDownloadButton() {
		if (formatComboBox.selectionModel.selectedItem == null) {
			downloadButton.disableProperty()
					.value = true
			return
		}
		try {
			val int = bitrateField.text.toInt()
		} catch (e: NumberFormatException) {
			downloadButton.disableProperty()
					.value = true
			return
		}
		val file = File(outputFolderField.text)
		if (file.exists() && file.isDirectory) {
			downloadButton.disableProperty()
					.value = false
			return
		} else {
			downloadButton.disableProperty()
					.value = true
		}
	}

	private fun handleDescriptionButton() {
		if (getDescriptionButton.graphic is ProgressIndicator) return
		urlField.styleClass -= "error"
		regexField.requestFocus()
		descriptionBox.disableProperty()
				.value = true
		regexButton.disableProperty()
				.value = true
		urlField.disableProperty()
				.value = true
		val oldtext = getDescriptionButton.graphic
		getDescriptionButton.text = ""
		val indicator = ProgressIndicator()
		indicator.maxHeight = 20.0
		indicator.maxWidth = 20.0
		getDescriptionButton.graphic = indicator
		val job = GlobalScope.launch {
			try {
				youtubeDL.url = urlField.text
				youtubeDL.loadJsonData()
				val description = youtubeDL.getProperty("description")
				descriptionBox.text = description
				regexButton.disableProperty()
						.value = false
				descriptionBox.isDisable = false
				urlField.styleClass -= "error"
			} catch (e: Exception) {
				Platform.runLater {
					urlField.requestFocus()
					urlField.selectAll()
					urlField.styleClass += "error"
					regexButton.disableProperty()
							.value = true
					descriptionBox.clear()
					descriptionBox.isDisable = true
				}
			} finally {
				urlField.disableProperty()
						.value = false
				regexField.styleClass -= "error"
				if (youtubeDL.jsonLoaded) getDescriptionButton.disableProperty()
						.value = false
				Platform.runLater {
					getDescriptionButton.graphic = oldtext
				}
			}
		}
		GlobalScope.launch {
			job.join()
			if (youtubeDL.jsonLoaded) youtubeDL.fetchAlbumArt()
		}
	}

	private fun handleRegexButton() {
		try {
			val pattern = SongRegex.inputToRegex(regexField.text)
			val matcher = pattern.matcher(descriptionBox.text + System.lineSeparator())
			songs = SongRegex.matchSongs(matcher)
			secondPaneSwitch()
			regexField.styleClass -= "error"
		} catch (e: Exception) {
			e.printStackTrace()
			regexField.styleClass += "error"
			Platform.runLater {
				regexField.requestFocus()
				regexField.selectAll()
			}
			return
		}
	}
}