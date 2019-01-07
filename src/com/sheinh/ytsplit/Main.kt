package com.sheinh.ytsplit

import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage
import javafx.scene.Parent
import javafx.fxml.FXMLLoader



class MainApp : Application() {
    lateinit var controller: Controller
    override fun start(stage: Stage) {
        controller = Controller(stage)
        val fxmlLoader = FXMLLoader(javaClass.getResource("/Layout.fxml"))
        fxmlLoader.setController(controller)
        val root = fxmlLoader.load<Any>() as Parent
        stage.title = "YouTube Split"
        stage.scene = Scene(root)
        stage.isResizable = false;
        stage.show()
    }
}

fun main(args : Array<String>){
    Application.launch(MainApp::class.java,*args)
}