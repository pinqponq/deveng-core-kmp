package core.serverdriven.presentation.component

internal object ServerDrivenScreenTestLayout {
    const val SHUTTER_ID = "camera_shutter"
    const val SHUTTER_TITLE = "Camera settings"
    const val SWITCH_ID = "record_switch"
    const val TABS_ID = "main_tabs"
    const val NETWORK_TAB_ID = "network_tab"
    const val NETWORK_TAB_TITLE = "Network"
    const val TEXT_FIELD_ID = "camera_name_text_field"
    const val TEXT_FIELD_TITLE = "Camera name"
    const val TEXT_FIELD_VALUE = "Entrance"
    const val DROPDOWN_ID = "resolution_dropdown"
    const val DROPDOWN_VALUE = "1080p"
    const val DROPDOWN_OTHER_OPTION = "4K"
    const val SLIDER_ID = "brightness_slider"
    const val SLIDER_TITLE = "Brightness"

    const val JSON = """
        {
          "components": [
            {
              "type": "shutter", "id": "$SHUTTER_ID", "title": "$SHUTTER_TITLE", "description": "Basic camera settings",
              "components": [
                {
                  "type": "textfield", "id": "$TEXT_FIELD_ID", "title": "$TEXT_FIELD_TITLE",
                  "currentValue": "$TEXT_FIELD_VALUE", "defaultValue": "", "factoryValue": "", "hintMessage": "Parking 1"
                },
                {
                  "type": "row", "id": "record_row",
                  "components": [
                    { "type": "switch", "id": "$SWITCH_ID", "title": "Recording", "currentValue": false },
                    {
                      "type": "dropdown", "id": "$DROPDOWN_ID", "title": "Resolution",
                      "currentValue": "$DROPDOWN_VALUE", "defaultValue": "1080p", "factoryValue": "720p",
                      "options": ["720p", "$DROPDOWN_VALUE", "$DROPDOWN_OTHER_OPTION"]
                    }
                  ]
                }
              ]
            },
            { "type": "divider", "id": "camera_divider" },
            {
              "type": "tabs", "id": "$TABS_ID",
              "tabs": [
                {
                  "type": "tab", "id": "image_tab", "title": "Image",
                  "components": [
                    {
                      "type": "slider", "id": "$SLIDER_ID", "title": "$SLIDER_TITLE",
                      "minValue": 0, "maxValue": 100, "currentValue": 85, "defaultValue": 50, "factoryValue": 50
                    }
                  ]
                },
                { "type": "tab", "id": "$NETWORK_TAB_ID", "title": "$NETWORK_TAB_TITLE", "components": [] }
              ]
            }
          ],
          "dependencies": [
            {
              "sourceId": "$SWITCH_ID", "targetId": "$DROPDOWN_ID", "operator": "EQUALS",
              "triggerValue": "false", "effectType": "EDITABLE", "effectValue": false
            },
            {
              "sourceId": "$TABS_ID", "targetId": "$SWITCH_ID", "operator": "EQUALS",
              "triggerValue": "$NETWORK_TAB_ID", "effectType": "EDITABLE", "effectValue": false
            },
            {
              "sourceId": "$TABS_ID", "targetId": "$TEXT_FIELD_ID", "operator": "EQUALS",
              "triggerValue": "$NETWORK_TAB_ID", "effectType": "VISIBILITY", "effectValue": false
            }
          ]
        }
    """
}
