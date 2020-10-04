package ru.skillbranch.skillarticles.extensions

fun String?.indexesOf(substr: String, ignoreCase: Boolean = true): List<Int> {
    val indexes = mutableListOf<Int>()
    var tmp = this?.indexOf(substr, 0, ignoreCase)
    while (tmp != -1 && substr != "") {
        indexes.add(tmp!!)
        tmp = this?.indexOf(substr, tmp + 1, ignoreCase)
    }
    return indexes
}
