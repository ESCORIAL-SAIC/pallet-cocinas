package com.escorial.pallet_cocinas

data class Product(
    val serial: String,
    val code: String,
    val maxCantByPallet: Int,
    var palletized: Boolean
) {
    companion object {
        private val kitchens = arrayListOf<Product>(
            Product("1", "01.01-18080", 8, false),
            Product("2", "01.01-18080", 8, false),
            Product("3", "01.01-18080", 8, false),
            Product("4", "01.01-18080", 8, false),
            Product("5", "01.01-18080", 8, false),
            Product("6", "01.01-18080",  8, false),
            Product("7", "01.01-18080",  8, false),
            Product("8", "01.01-18080",  8, false),
            Product("9", "01.01-18080",  8, false),
            Product("10", "01.01-18080",  8, false),
            Product("11", "01.01-00190",  8, false),
            Product("12", "01.01-00190",  8, false),
            Product("13", "01.01-00190",  8, false),
            Product("14", "01.01-00190",  8, false),
            Product("15", "01.01-00190",  8, false),
            Product("16", "01.01-00190",  8, false),
            Product("17", "01.01-00190",  8, false),
            Product("18", "01.01-00190",  8, false),
            Product("19", "01.01-00190",  8, false),
            Product("20", "01.01-18080",  8, false),
            Product("21", "01.01-18080",  8, false),
            Product("22", "01.01-18080",  8, false),
            Product("23", "01.01-18080",  8, false),
            Product("24", "01.01-18080",  8, false),
            Product("25", "01.01-00190",  8, false),
            Product("26", "01.01-00190",  8, false),
            Product("27", "01.01-00190",  8, false),
            Product("28", "01.01-00190",  8, false),
            Product("29", "01.01-00190",  8, false),
            Product("30", "01.01-00190",  8, false)
        )

        private val heaters = arrayListOf<Product>(
            Product("1", "01.04-30000",  12, false),
            Product("2", "01.04-30000",  12, false),
            Product("3", "01.04-30000",  12, false),
            Product("4", "01.04-30000",  12, false),
            Product("5", "01.04-30000",  12, false),
            Product("6", "01.04-30000",  12, false),
            Product("7", "01.04-30000",  12, false),
            Product("8", "01.04-30000",  12, false),
            Product("9", "01.04-30000",  12, false),
            Product("10", "01.04-30000",  12, false),
            Product("11", "01.04-30000",  12, false),
            Product("12", "01.04-30000",  12, false),
            Product("13", "01.04-30000",  12, false),
            Product("14", "01.04-30000",  12, false),
            Product("15", "01.04-30000",  12, false),
            Product("16", "01.04-30000",  12, false),
            Product("17", "01.04-30000",  12, false),
            Product("18", "01.04-30000",  12, false),
            Product("19", "01.04-30000",  12, false),
            Product("20", "01.04-30000",  12, false),
            Product("21", "01.04-30000",  12, false),
            Product("22", "01.04-30000",  12, false),
            Product("23", "01.04-30000",  12, false),
            Product("24", "01.04-30000",  12, false),
            Product("25", "01.04-30000",  12, false),
            Product("26", "01.04-30000",  12, false),
            Product("27", "01.04-30000",  12, false),
            Product("28", "01.04-30000",  12, false),
            Product("29", "01.04-30000",  12, false),
            Product("30", "01.04-30000",  12, false)
        )

        fun getKitchens(): ArrayList<Product> = kitchens
        fun getHeaters(): ArrayList<Product> = heaters

        fun updateStockedKitchen(serial: String, stocked: Boolean) {
            kitchens.find { it.serial == serial }?.palletized = stocked
        }

        fun updateStockedHeater(serial: String, stocked: Boolean) {
            heaters.find { it.serial == serial }?.palletized = stocked
        }
    }
}
