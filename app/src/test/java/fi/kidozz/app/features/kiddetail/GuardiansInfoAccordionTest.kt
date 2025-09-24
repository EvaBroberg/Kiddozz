package fi.kidozz.app.features.kiddetail

import fi.kidozz.app.data.models.Kid
import fi.kidozz.app.data.models.TrustedAdult
import org.junit.Test
import org.junit.Assert.*

class GuardiansInfoAccordionTest {

    @Test
    fun kidWithGuardians_hasCorrectData() {
        val testKid = createTestKidWithGuardians()
        
        assertEquals("1", testKid.id)
        assertEquals("Emma Johnson", testKid.full_name)
        assertEquals(2, testKid.trusted_adults.size)
        assertEquals("Sarah Johnson", testKid.trusted_adults[0].name)
        assertEquals("Mike Johnson", testKid.trusted_adults[1].name)
    }

    @Test
    fun kidWithoutGuardians_hasEmptyGuardiansList() {
        val testKid = createTestKidWithoutGuardians()
        
        assertEquals("2", testKid.id)
        assertEquals("Liam Smith", testKid.full_name)
        assertTrue(testKid.trusted_adults.isEmpty())
    }

    @Test
    fun trustedAdult_hasAllRequiredFields() {
        val guardian = TrustedAdult(
            name = "Sarah Johnson",
            email = "sarah.johnson@email.com",
            phone_num = "+1234567890",
            address = "123 Main St, City"
        )
        
        assertEquals("Sarah Johnson", guardian.name)
        assertEquals("sarah.johnson@email.com", guardian.email)
        assertEquals("+1234567890", guardian.phone_num)
        assertEquals("123 Main St, City", guardian.address)
    }

    @Test
    fun trustedAdult_handlesNullFields() {
        val guardian = TrustedAdult(
            name = "John Doe",
            email = null,
            phone_num = null,
            address = null
        )
        
        assertEquals("John Doe", guardian.name)
        assertNull(guardian.email)
        assertNull(guardian.phone_num)
        assertNull(guardian.address)
    }

    private fun createTestKidWithGuardians(): Kid {
        return Kid(
            id = "1",
            full_name = "Emma Johnson",
            dob = "2020-03-15",
            group_id = "1",
            daycare_id = "test-daycare-id",
            trusted_adults = listOf(
                TrustedAdult(
                    name = "Sarah Johnson",
                    email = "sarah.johnson@email.com",
                    phone_num = "+1234567890",
                    address = "123 Main St, City"
                ),
                TrustedAdult(
                    name = "Mike Johnson",
                    email = "mike.johnson@email.com",
                    phone_num = "+1234567891",
                    address = "123 Main St, City"
                )
            ),
            attendance = "out"
        )
    }

    private fun createTestKidWithoutGuardians(): Kid {
        return Kid(
            id = "2",
            full_name = "Liam Smith",
            dob = "2019-08-22",
            group_id = "1",
            daycare_id = "test-daycare-id",
            trusted_adults = emptyList(),
            attendance = "in-care"
        )
    }
}
