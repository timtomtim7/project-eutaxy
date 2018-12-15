package blue.sparse.eutaxy

import blue.sparse.engine.SparseEngine
import blue.sparse.engine.window.Window

fun main(args: Array<String>) {
	SparseEngine.start(Window("Project Eutaxy", 1280 , 720, true, false, true), Eutaxy::class)
}