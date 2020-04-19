package tech.favs.ebs.util

//class BatchInsertUpdateOnDuplicate(table: Table, private val onDupUpdate: Array<out Column<*>>) : BatchInsertStatement(table, false) {
//    override fun prepareSQL(transaction: Transaction): String {
//        val onUpdateSQL = if (onDupUpdate.isNotEmpty()) {
//            " ON DUPLICATE KEY UPDATE " + onDupUpdate.joinToString { "${transaction.identity(it)}=VALUES(${transaction.identity(it)})" }
//        } else ""
//        return super.prepareSQL(transaction) + onUpdateSQL
//    }
//}
//
//fun <T : Table, E> T.batchInsertOnDuplicateKeyUpdate(data: List<E>, vararg onDupUpdateColumns: Column<*>, body: T.(BatchInsertUpdateOnDuplicate, E) -> Unit) {
//    if (data.isNotEmpty()) {
//        val insert = BatchInsertUpdateOnDuplicate(this, onDupUpdateColumns)
//        data.forEach {
//            insert.addBatch()
//            body(insert, it)
//        }
//        TransactionManager.current().exec(insert)
//    }
//}

//fun <T : Table> T.insertOrUpdate(vararg onDuplicateUpdateKeys: Column<*>, body: T.(InsertStatement<Number>) -> Unit) =
//        InsertOrUpdate<Number>(onDuplicateUpdateKeys,this).apply {
//            body(this)
//            execute(TransactionManager.current())
//        }
//
//class InsertOrUpdate<Key : Any>(
//        private val onDuplicateUpdateKeys: Array< out Column<*>>,
//        table: Table,
//        isIgnore: Boolean = false
//) : InsertStatement<Key>(table, isIgnore) {
//    override fun prepareSQL(transaction: Transaction): String {
//        val onUpdateSQL = if(onDuplicateUpdateKeys.isNotEmpty()) {
//            " ON DUPLICATE KEY UPDATE " + onDuplicateUpdateKeys.joinToString { "${transaction.identity(it)}=VALUES(${transaction.identity(it)})" }
//        } else ""
//        return super.prepareSQL(transaction) + onUpdateSQL
//    }
//}
