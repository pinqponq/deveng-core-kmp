package figmasync.generator

import kotlin.test.Test
import kotlin.test.assertEquals

class TemplateGeneratorTest {

    private val labeledSwitchUrl = "https://www.figma.com/design/sJoAsKB4qqqrwvHRlowppo/Design-System?node-id=148-87"
    private val customIconButtonUrl = "https://www.figma.com/design/sJoAsKB4qqqrwvHRlowppo/Design-System?node-id=150-63"

    @Test
    fun `generates LabeledSwitch dynamic template with text and boolean bindings`() {
        val schema = testSchema()
        val component = schema.components.first { it.componentName == "LabeledSwitch" }
        val output = renderTemplate(component, labeledSwitchUrl)
        val expected = this::class.java.getResource("/golden/LabeledSwitch.figma.template.js")!!.readText().trim()
        assertEquals(expected, output.trim())
    }

    @Test
    fun `generates CustomIconButton dynamic template with variant and instance swap`() {
        val schema = testSchema()
        val component = schema.components.first { it.componentName == "CustomIconButton" }
        val output = renderTemplate(component, customIconButtonUrl)
        val expected = this::class.java.getResource("/golden/CustomIconButton.figma.template.js")!!.readText().trim()
        assertEquals(expected, output.trim())
    }
}
