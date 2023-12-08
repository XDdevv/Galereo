package uz.xd.galleryofsky.models

class Story {
    var id: String = ""
    var imageUrl: String = ""
    constructor()
    constructor(id: String, imageUrl: String) {
        this.id = id
        this.imageUrl = imageUrl
    }
}