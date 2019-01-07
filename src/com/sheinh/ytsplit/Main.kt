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
        var fxmlLoader = FXMLLoader(javaClass.getResource("/FirstPane.fxml"))
        fxmlLoader.setController(controller)
        var root = fxmlLoader.load<Any>() as Parent
        controller.firstPaneInit()
        controller.firstPane = root
        stage.title = "YouTube Split"
        stage.scene = Scene(root)
        stage.isResizable = false;
        stage.show()
        fxmlLoader = FXMLLoader(javaClass.getResource("/SecondPane.fxml"))
        fxmlLoader.setController(controller)
        root = fxmlLoader.load<Any>() as Parent
        controller.secondPane = root
    }
}

fun main(args : Array<String>){
    Application.launch(MainApp::class.java,*args)
}