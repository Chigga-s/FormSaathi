package com.formsaathi.formengine

import com.formsaathi.model.FieldType
import org.junit.Assert.assertEquals
import org.junit.Test

class FieldMapperTest {

    private val mapper = FieldMapper()
    
    @Test
    fun exactAliasMapsMobile() {
        val result = mapper.map("Mobile Number")

        assertEquals(
            FieldType.MOBILE,
            result.type
        )
    }
    @Test
    fun editDistanceMapsMobileTypo() {
        val result = mapper.map("Moblle Number")

        assertEquals(
            FieldType.MOBILE,
            result.type
        )
    }
    @Test
    fun unrelatedTextReturnsUnknown() {
        val result = mapper.map(
            "Please read all instructions carefully"
        )

        assertEquals(
            FieldType.UNKNOWN,
            result.type
        )
    }
    @Test
    fun motherNameDoesNotBecomeFullName() {
        val result = mapper.map("Mother's Name")

        assertEquals(
            FieldType.MOTHER_NAME,
            result.type
        )
    }
}
