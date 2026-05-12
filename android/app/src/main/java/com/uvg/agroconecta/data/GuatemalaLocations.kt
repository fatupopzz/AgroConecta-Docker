package com.uvg.agroconecta.data

object GuatemalaLocations {

    val departamentos: List<String> = listOf(
        "Alta Verapaz", "Baja Verapaz", "Chimaltenango", "Chiquimula",
        "El Progreso", "Escuintla", "Guatemala", "Huehuetenango",
        "Izabal", "Jalapa", "Jutiapa", "Petén", "Quetzaltenango",
        "Quiché", "Retalhuleu", "Sacatepéquez", "San Marcos",
        "Santa Rosa", "Sololá", "Suchitepéquez", "Totonicapán", "Zacapa"
    )

    private val municipiosPorDepartamento: Map<String, List<String>> = mapOf(
        "Alta Verapaz" to listOf(
            "Cobán", "Chahal", "Chisec", "Fray Bartolomé de las Casas",
            "Lanquín", "Panzós", "Raxruhá", "San Cristóbal Verapaz",
            "San Juan Chamelco", "San Pedro Carchá", "Santa Cruz Verapaz",
            "Santa María Cahabón", "Senahú", "Tactic", "Tamahú", "Tucurú"
        ),
        "Baja Verapaz" to listOf(
            "Salamá", "Cubulco", "Granados", "El Chol", "Purulhá",
            "Rabinal", "San Jerónimo", "San Miguel Chicaj"
        ),
        "Chimaltenango" to listOf(
            "Chimaltenango", "Acatenango", "Comalapa", "El Tejar",
            "Parramos", "Patzicía", "Patzún", "Pochuta",
            "San Andrés Itzapa", "San José Poaquil", "San Martín Jilotepeque",
            "Santa Apolonia", "Santa Cruz Balanyá", "Tecpán Guatemala",
            "Yepocapa", "Zaragoza"
        ),
        "Chiquimula" to listOf(
            "Chiquimula", "Camotán", "Concepción Las Minas", "Esquipulas",
            "Ipala", "Jocotán", "Olopa", "Quezaltepeque",
            "San Jacinto", "San José La Arada", "San Juan Ermita"
        ),
        "El Progreso" to listOf(
            "Guastatoya", "El Jícaro", "Morazán", "San Agustín Acasaguastlán",
            "San Antonio La Paz", "San Cristóbal Acasaguastlán", "Sanarate", "Sansare"
        ),
        "Escuintla" to listOf(
            "Escuintla", "Guanagazapa", "Iztapa", "La Democracia",
            "La Gomera", "Masagua", "Nueva Concepción", "Palín",
            "San José", "San Vicente Pacaya", "Santa Lucía Cotzumalguapa",
            "Sipacate", "Siquinalá", "Tiquisate"
        ),
        "Guatemala" to listOf(
            "Guatemala", "Amatitlán", "Chinautla", "Chuarrancho",
            "Fraijanes", "Mixco", "Palencia", "San José del Golfo",
            "San José Pinula", "San Juan Sacatepéquez", "San Miguel Petapa",
            "San Pedro Ayampuc", "San Pedro Sacatepéquez", "San Raymundo",
            "Santa Catarina Pinula", "Villa Canales", "Villa Nueva"
        ),
        "Huehuetenango" to listOf(
            "Huehuetenango", "Aguacatán", "Chiantla", "Colotenango",
            "Concepción Huista", "Cuilco", "Jacaltenango", "La Democracia",
            "La Libertad", "Malacatancito", "Nentón", "San Antonio Huista",
            "San Gaspar Ixchil", "San Ildefonso Ixtahuacán", "San Juan Atitán",
            "San Juan Ixcoy", "San Mateo Ixtatán", "San Miguel Acatán",
            "San Pedro Necta", "San Pedro Soloma", "San Rafael La Independencia",
            "San Rafael Petzal", "San Sebastián Coatán", "San Sebastián Huehuetenango",
            "Santa Ana Huista", "Santa Bárbara", "Santa Cruz Barillas",
            "Santa Eulalia", "Santiago Chimaltenango", "Tectitán", "Todos Santos Cuchumatán",
            "Unión Cantinil"
        ),
        "Izabal" to listOf(
            "Puerto Barrios", "El Estor", "Livingston", "Los Amates", "Morales"
        ),
        "Jalapa" to listOf(
            "Jalapa", "Mataquescuintla", "Monjas", "San Carlos Alzatate",
            "San Luis Jilotepeque", "San Manuel Chaparrón", "San Pedro Pinula"
        ),
        "Jutiapa" to listOf(
            "Jutiapa", "Agua Blanca", "Asunción Mita", "Atescatempa",
            "Comapa", "Conguaco", "El Adelanto", "El Progreso",
            "Jalpatagua", "Jerez", "Moyuta", "Pasaco",
            "Quesada", "San José Acatempa", "Santa Catarina Mita",
            "Yupiltepeque", "Zapotitlán"
        ),
        "Petén" to listOf(
            "Flores", "Dolores", "La Libertad", "Melchor de Mencos",
            "Poptún", "San Andrés", "San Benito", "San Francisco",
            "San José", "San Luis", "Santa Ana", "Sayaxché",
            "Las Cruces", "El Chal"
        ),
        "Quetzaltenango" to listOf(
            "Quetzaltenango", "Almolonga", "Cabricán", "Cajolá",
            "Cantel", "Coatepeque", "Colomba", "Concepción Chiquirichapa",
            "El Palmar", "Flores Costa Cuca", "Génova", "Huitán",
            "La Esperanza", "Olintepeque", "Ostuncalco", "Palestina de los Altos",
            "Salcajá", "San Carlos Sija", "San Francisco La Unión",
            "San Martín Sacatepéquez", "San Mateo", "San Miguel Sigüilá",
            "Sibilia", "Zunil"
        ),
        "Quiché" to listOf(
            "Santa Cruz del Quiché", "Canillá", "Chajul", "Chicamán",
            "Chiché", "Chichicastenango", "Chinique", "Cunén",
            "Ixcán", "Joyabaj", "Nebaj", "Pachalum",
            "Patzité", "Sacapulas", "San Andrés Sajcabajá", "San Antonio Ilotenango",
            "San Bartolomé Jocotenango", "San Juan Cotzal", "San Pedro Jocopilas",
            "Uspantán", "Zacualpa"
        ),
        "Retalhuleu" to listOf(
            "Retalhuleu", "Champerico", "El Asintal", "Nuevo San Carlos",
            "San Andrés Villa Seca", "San Felipe", "San Martín Zapotitlán",
            "San Sebastián", "Santa Cruz Muluá"
        ),
        "Sacatepéquez" to listOf(
            "Antigua Guatemala", "Alotenango", "Ciudad Vieja", "Jocotenango",
            "Magdalena Milpas Altas", "Pastores", "San Antonio Aguas Calientes",
            "San Bartolomé Milpas Altas", "San Lucas Sacatepéquez",
            "San Miguel Dueñas", "Santa Catarina Barahona", "Santa Lucía Milpas Altas",
            "Santa María de Jesús", "Santiago Sacatepéquez", "Santo Domingo Xenacoj",
            "Sumpango"
        ),
        "San Marcos" to listOf(
            "San Marcos", "Ayutla", "Catarina", "Comitancillo",
            "Concepción Tutuapa", "El Quetzal", "El Rodeo", "El Tumbador",
            "Esquipulas Palo Gordo", "Ixchiguán", "La Reforma", "Malacatán",
            "Nuevo Progreso", "Ocós", "Pajapita", "Río Blanco",
            "San Antonio Sacatepéquez", "San Cristóbal Cucho",
            "San José Ojetenam", "San Lorenzo", "San Miguel Ixtahuacán",
            "San Pablo", "San Pedro Sacatepéquez", "San Rafael Pie de la Cuesta",
            "Sibinal", "Sipacapa", "Tacaná", "Tajumulco", "Tejutla"
        ),
        "Santa Rosa" to listOf(
            "Cuilapa", "Barberena", "Casillas", "Chiquimulilla",
            "Guazacapán", "Nueva Santa Rosa", "Oratorio", "Pueblo Nuevo Viñas",
            "San Juan Tecuaco", "San Rafael Las Flores", "Santa Cruz Naranjo",
            "Santa María Ixhuatán", "Santa Rosa de Lima", "Taxisco"
        ),
        "Sololá" to listOf(
            "Sololá", "Concepción", "Nahualá", "Panajachel",
            "San Andrés Semetabaj", "San Antonio Palopó", "San José Chacayá",
            "San Juan La Laguna", "San Lucas Tolimán", "San Marcos La Laguna",
            "San Pablo La Laguna", "San Pedro La Laguna", "Santa Catarina Ixtahuacán",
            "Santa Catarina Palopó", "Santa Clara La Laguna", "Santa Cruz La Laguna",
            "Santa Lucía Utatlán", "Santa María Visitación", "Santiago Atitlán"
        ),
        "Suchitepéquez" to listOf(
            "Mazatenango", "Chicacao", "Cuyotenango", "Patulul",
            "Pueblo Nuevo", "Río Bravo", "Samayac", "San Antonio Suchitepéquez",
            "San Bernardino", "San Francisco Zapotitlán", "San Gabriel",
            "San José El Idolo", "San José La Maquina", "San Juan Bautista",
            "San Lorenzo", "San Miguel Panán", "San Pablo Jocopilas",
            "Santa Bárbara", "Santo Domingo Suchitepéquez", "Santo Tomás La Unión",
            "Zunilito"
        ),
        "Totonicapán" to listOf(
            "Totonicapán", "Momostenango", "San Andrés Xecul", "San Bartolo",
            "San Cristóbal Totonicapán", "San Francisco El Alto", "Santa Lucía La Reforma",
            "Santa María Chiquimula"
        ),
        "Zacapa" to listOf(
            "Zacapa", "Cabañas", "Estanzuela", "Gualán",
            "Huité", "La Unión", "Río Hondo", "San Diego",
            "San Jorge", "Teculután", "Usumatlán"
        )
    )

    fun municipiosDe(departamento: String?): List<String> {
        if (departamento.isNullOrBlank()) return emptyList()
        return municipiosPorDepartamento[departamento] ?: emptyList()
    }
}