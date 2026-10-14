package core.serverdriven.presentation.component

internal object ServerDrivenInputTestLayout {
    const val LOCK_SWITCH_ID = "lock_switch"
    const val LABEL_ID = "intro_label"
    const val LABEL_TEXT = "Settings apply after a restart"
    const val SECTION_ID = "schedule_section"
    const val SECTION_TITLE = "Schedule"
    const val SEGMENTED_ID = "mode_segmented"
    const val SEGMENTED_VALUE = "Day"
    const val SEGMENTED_OTHER_OPTION = "Night"
    const val RADIO_ID = "quality_radio"
    const val RADIO_TITLE = "Quality"
    const val RADIO_VALUE = "High"
    const val RADIO_OTHER_OPTION = "Low"
    const val RATING_ID = "service_rating"
    const val DATE_ID = "start_date"
    const val DATE_TITLE = "Start date"
    const val DATE_VALUE = "2026-03-15"

    const val LABEL_JSON = """{ "type": "label", "id": "$LABEL_ID", "text": "$LABEL_TEXT" }"""

    const val SEGMENTED_JSON = """
        {
          "type": "segmented", "id": "$SEGMENTED_ID", "title": "Mode", "description": "Camera profile",
          "currentValue": "$SEGMENTED_VALUE", "defaultValue": "$SEGMENTED_VALUE", "factoryValue": "$SEGMENTED_VALUE",
          "options": ["$SEGMENTED_VALUE", "$SEGMENTED_OTHER_OPTION", "Auto"]
        }
    """

    const val RADIO_JSON = """
        {
          "type": "radio", "id": "$RADIO_ID", "title": "$RADIO_TITLE", "description": "Higher quality uses more data",
          "currentValue": "$RADIO_VALUE", "defaultValue": "$RADIO_VALUE", "factoryValue": "$RADIO_OTHER_OPTION",
          "options": ["$RADIO_OTHER_OPTION", "$RADIO_VALUE"]
        }
    """

    const val RATING_JSON = """
        {
          "type": "rating", "id": "$RATING_ID", "title": "Service", "description": "How was the setup?",
          "currentValue": 3, "defaultValue": 0, "factoryValue": 0
        }
    """

    const val DATE_JSON = """
        {
          "type": "date", "id": "$DATE_ID", "title": "$DATE_TITLE",
          "currentValue": "$DATE_VALUE", "defaultValue": "", "factoryValue": ""
        }
    """

    const val SECTION_JSON = """
        {
          "type": "section", "id": "$SECTION_ID", "title": "$SECTION_TITLE", "description": "When the camera records",
          "components": [$LABEL_JSON, $DATE_JSON]
        }
    """

    const val JSON = """
        {
          "components": [
            { "type": "switch", "id": "$LOCK_SWITCH_ID", "title": "Lock settings", "currentValue": false },
            $SEGMENTED_JSON,
            $RADIO_JSON,
            $RATING_JSON,
            $SECTION_JSON
          ],
          "dependencies": [
            {
              "sourceId": "$LOCK_SWITCH_ID", "targetId": "$SEGMENTED_ID", "operator": "EQUALS",
              "triggerValue": "true", "effectType": "EDITABLE", "effectValue": false
            },
            {
              "sourceId": "$LOCK_SWITCH_ID", "targetId": "$RADIO_ID", "operator": "EQUALS",
              "triggerValue": "true", "effectType": "EDITABLE", "effectValue": false
            },
            {
              "sourceId": "$LOCK_SWITCH_ID", "targetId": "$RATING_ID", "operator": "EQUALS",
              "triggerValue": "true", "effectType": "EDITABLE", "effectValue": false
            },
            {
              "sourceId": "$LOCK_SWITCH_ID", "targetId": "$DATE_ID", "operator": "EQUALS",
              "triggerValue": "true", "effectType": "EDITABLE", "effectValue": false
            }
          ]
        }
    """

    fun layoutOf(componentJson: String): String = """{ "components": [$componentJson] }"""
}
