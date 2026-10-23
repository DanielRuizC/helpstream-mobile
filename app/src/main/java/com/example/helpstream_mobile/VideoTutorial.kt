package com.example.helpstream_mobile

data class VideoTutorial(
    val id: Int,
    val titulo: String,
    val descripcion: String,
    val url_video: String,
    val tags: String,
    val fecha_subida: String,
    val reproducciones: Int = 0,
    val casos_resueltos: Int = 0
)