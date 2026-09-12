package com.formsaathi.formengine

import com.formsaathi.model.FieldType

object FieldAliases {

    val aliases: Map<FieldType, Set<String>> = mapOf(
        FieldType.FULL_NAME to setOf(
            "full name",
            "name of applicant",
            "applicant name",
            "name of the applicant",
        ),
        FieldType.FATHER_NAME to setOf(
            "father's name",
            "name of father",
            "father name",
            "name of the father",
        ),
        FieldType.MOTHER_NAME to setOf(
            "mother's name",
            "name of mother",
            "mother name",
            "name of the mother",
        ),
        FieldType.DATE_OF_BIRTH to setOf(
            "date of birth",
            "dob",
            "birth date",
            "birthdate",
        ),
        FieldType.GENDER to setOf(
            "gender",
            "sex",
        ),
        FieldType.MOBILE to setOf(
            "mobile number",
            "phone number",
            "contact number",
            "mobile no",
            "phone no",
        ),
        FieldType.EMAIL to setOf(
            "email address",
            "email id",
            "e-mail",
            "email",
            "e-mail address",
        ),
        FieldType.AADHAAR to setOf(
            "aadhaar number",
            "aadhar number",
            "aadhaar no",
            "aadhar no",
            "aadhaar id",
            "aadhar id",
            "aadhaar",
            "aadhar",
        ),
        FieldType.PERMANENT_ADDRESS to setOf(
            "permanent address",
            "address of residence",
            "residential address",
            "address of permanent residence",
            "permanent residence address",            
        ),
        FieldType.CURRENT_ADDRESS to setOf(
            "current address",
            "address of current residence",
            "present address",
            "address of present residence",
            "current residence address",
            "communication address",
        ),
        FieldType.STATE to setOf(
            "state",
            "state name",
        ),
        FieldType.DISTRICT to setOf(
            "district",
            "district name",
        ),
        FieldType.PINCODE to setOf(
            "pincode",
            "postal code",
            "zip code",
            "pin code",
        ),
        FieldType.CATEGORY to setOf(
            "category",
            "caste category",
            "social category",
        ),
        FieldType.ANNUAL_INCOME to setOf(
            "annual income",
            "yearly income",
            "income per year",
            "income per annum",
            "family income",
            "family annual income",
        ),
        FieldType.PHOTO to setOf(
            "photo",
            "photograph",
            "passport size photo",
            "passport photo",
        ),
        FieldType.SIGNATURE to setOf(
            "signature",
            "sign",
            "applicant signature",
            "signature of applicant",
        )
    )
}