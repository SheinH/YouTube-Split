package com.sheinhtike.ytsplit

import javafx.event.EventHandler
import javafx.geometry.Side
import javafx.scene.control.ContextMenu
import javafx.scene.control.CustomMenuItem
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import java.util.*
import java.util.regex.Pattern


/**
 * This class is a TextField which implements an "autocomplete" functionality, based on a supplied list of entries.
 * @author Caleb Brinkman
 */
class AutoCompleteTextField : TextField() {
	/** The existing autocomplete entries.  */
	/**
	 * Get the existing set of autocomplete entries.
	 * @return The existing autocomplete entries.
	 */
	val entries : SortedSet<String>
	/** The popup used to select an entry.  */
	private val entriesPopup : ContextMenu

	/** Construct a new AutoCompleteTextField.  */
	init {
		entries = TreeSet()
		entriesPopup = ContextMenu()
		textProperty().addListener { observableValue, s, s2 ->
			if (text.isEmpty()) {
				entriesPopup.hide()
			} else {
				val searchResult = LinkedList<String>()
				val matcher = Pattern.compile("(\\S+)\$").matcher(text)
				matcher.find()
				try {
					val lastword = matcher.group(1).toUpperCase()
					searchResult.addAll(entries.filter { it.indexOf(lastword) > -1 })
					if (entries.size > 0) {
						populatePopup(searchResult, lastword)
						if (!entriesPopup.isShowing) {
							entriesPopup.show(this@AutoCompleteTextField, Side.BOTTOM, 0.0, 0.0)
						}
					} else {
						entriesPopup.hide()
					}
				} catch (e : Exception) {
				}
			}
		}

		focusedProperty().addListener { observableValue, aBoolean, aBoolean2 -> entriesPopup.hide() }
	}

	private fun buildTextFlow(text : String, filter : String) : TextFlow {
		val filterIndex = text.toLowerCase().indexOf(filter.toLowerCase())
		val textBefore = Text(text.substring(0, filterIndex))
		val textAfter = Text(text.substring(filterIndex + filter.length))
		val textFilter = Text(
			text.substring(
				filterIndex, filterIndex + filter.length
			)
		) //instead of "filter" to keep all "case sensitive"
		textFilter.fill = Color.RED
		textFilter.font = Font.font("Helvetica", FontWeight.BOLD, 13.0)
		return TextFlow(textBefore, textFilter, textAfter)
	}

	private fun populatePopup(searchResult : List<String>, searchRequest : String) {
		val menuItems = LinkedList<CustomMenuItem>()
		// If you'd like more entries, modify this line.
		val maxEntries = 10
		val count = Math.min(searchResult.size, maxEntries)
		for (i in 0 until count) {
			val result = searchResult[i]
			//label with graphic (text flow) to highlight founded subtext in suggestions
			val entryLabel = Label()
			entryLabel.graphic = buildTextFlow(result, searchRequest)
			entryLabel.prefHeight = 10.0  //don't sure why it's changed with "graphic"

			val item = CustomMenuItem(entryLabel, true)
			item.onAction = EventHandler {
				text = text.replace(Regex("(\\S+)\$"), result)
				positionCaret(text.length)
				entriesPopup.hide()
			}
			menuItems.add(item)
		}
		entriesPopup.items.clear()
		entriesPopup.items.addAll(menuItems)
	}
}