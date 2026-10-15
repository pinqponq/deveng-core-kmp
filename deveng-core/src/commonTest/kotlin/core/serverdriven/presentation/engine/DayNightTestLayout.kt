package core.serverdriven.presentation.engine

internal object DayNightTestLayout {
    const val TABS_ID = "tab_id"
    const val DAY_TAB_ID = "day_tab"
    const val NIGHT_TAB_ID = "night_tab"
    const val SCHEDULE_SWITCH_ID = "schedule_switch"
    const val START_TIME_TEXT_FIELD_ID = "start_time_textfield"
    const val END_TIME_TEXT_FIELD_ID = "end_time_textfield"
    const val DAY_START_TIME = "06:30:00"
    const val NIGHT_START_TIME = "18:30:00"

    fun json(tabsType: String = "tabs"): String {
        return """
            {
              "components": [
                {
                  "type": "$tabsType",
                  "id": "tab_id",
                  "tabs": [
                    {
                      "id": "day_tab",
                      "title": "Day Profile (Config 0)",
                      "components": [
                        {
                          "type": "slider",
                          "id": "brightness_slider",
                          "title": "Brightness",
                          "description": "Configure brightness setting, default value is 40",
                          "minValue": 0,
                          "maxValue": 100,
                          "currentValue": 43,
                          "defaultValue": 40,
                          "factoryValue": 50
                        },
                        {
                          "type": "slider",
                          "id": "contrast_slider",
                          "title": "Contrast",
                          "description": "Configure contrast setting, default value is 40",
                          "minValue": 0,
                          "maxValue": 100,
                          "currentValue": 43,
                          "defaultValue": 40,
                          "factoryValue": 50
                        },
                        {
                          "type": "slider",
                          "id": "hue_slider",
                          "title": "Hue",
                          "description": "Configure hue setting, default value is 40",
                          "minValue": 0,
                          "maxValue": 100,
                          "currentValue": 43,
                          "defaultValue": 40,
                          "factoryValue": 50
                        },
                        {
                          "type": "slider",
                          "id": "saturation_slider",
                          "title": "Saturation",
                          "description": "Configure saturation setting, default value is 40",
                          "minValue": 0,
                          "maxValue": 100,
                          "currentValue": 43,
                          "defaultValue": 40,
                          "factoryValue": 50
                        },
                        {
                          "type": "divider",
                          "id": "divider_id"
                        },
                        {
                          "type": "switch",
                          "id": "schedule_switch",
                          "title": "Schedule Settings",
                          "description": "Enable Schedule for this Profile",
                          "currentValue": true
                        },
                        {
                          "type": "row",
                          "id": "row_id",
                          "components": [
                            {
                              "type": "textfield",
                              "id": "start_time_textfield",
                              "title": "Start Time",
                              "description": "Configure start time settings, default value is 06:00:00",
                              "currentValue": "06:30:00",
                              "defaultValue": "06:00:00",
                              "factoryValue": "06:00:00",
                              "errorMessage": "There is an error",
                              "hintMessage": "Enter Start Time"
                            },
                            {
                              "type": "textfield",
                              "id": "end_time_textfield",
                              "title": "End Time",
                              "description": "Configure end time settings, default value is 18:00:00",
                              "currentValue": "18:30:00",
                              "defaultValue": "18:00:00",
                              "factoryValue": "18:00:00",
                              "errorMessage": "There is an error",
                              "hintMessage": "Enter End Time"
                            }
                          ]
                        }
                      ]
                    },
                    {
                      "id": "night_tab",
                      "title": "Night Profile (Config 1)",
                      "components": [
                        {
                          "type": "slider",
                          "id": "brightness_slider",
                          "title": "Brightness",
                          "description": "Configure brightness setting, default value is 40",
                          "minValue": 0,
                          "maxValue": 100,
                          "currentValue": 43,
                          "defaultValue": 40,
                          "factoryValue": 50
                        },
                        {
                          "type": "slider",
                          "id": "contrast_slider",
                          "title": "Contrast",
                          "description": "Configure contrast setting, default value is 40",
                          "minValue": 0,
                          "maxValue": 100,
                          "currentValue": 43,
                          "defaultValue": 40,
                          "factoryValue": 50
                        },
                        {
                          "type": "slider",
                          "id": "hue_slider",
                          "title": "Hue",
                          "description": "Configure hue setting, default value is 40",
                          "minValue": 0,
                          "maxValue": 100,
                          "currentValue": 43,
                          "defaultValue": 40,
                          "factoryValue": 50
                        },
                        {
                          "type": "slider",
                          "id": "saturation_slider",
                          "title": "Saturation",
                          "description": "Configure saturation setting, default value is 40",
                          "minValue": 0,
                          "maxValue": 100,
                          "currentValue": 43,
                          "defaultValue": 40,
                          "factoryValue": 50
                        },
                        {
                          "type": "divider",
                          "id": "divider_2_id"
                        },
                        {
                          "type": "switch",
                          "id": "schedule_switch",
                          "title": "Schedule Settings",
                          "description": "Enable Schedule for this Profile",
                          "currentValue": true
                        },
                        {
                          "type": "row",
                          "id": "row_2_id",
                          "components": [
                            {
                              "type": "textfield",
                              "id": "start_time_textfield",
                              "title": "Start Time",
                              "description": "Configure start time settings, default value is 18:00:00",
                              "currentValue": "18:30:00",
                              "defaultValue": "18:00:00",
                              "factoryValue": "18:00:00",
                              "errorMessage": "There is an error",
                              "hintMessage": "Enter Start Time"
                            },
                            {
                              "type": "textfield",
                              "id": "end_time_textfield",
                              "title": "End Time",
                              "description": "Configure end time settings, default value is 06:00:00",
                              "currentValue": "06:30:00",
                              "defaultValue": "06:00:00",
                              "factoryValue": "06:00:00",
                              "errorMessage": "There is an error",
                              "hintMessage": "Enter End Time"
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
              ],
              "dependencies": [
                {
                  "sourceId": "schedule_switch",
                  "targetId": "start_time_textfield",
                  "operator": "EQUALS",
                  "triggerValue": "true",
                  "effectType": "EDITABLE",
                  "effectValue": true
                },
                {
                  "sourceId": "schedule_switch",
                  "targetId": "end_time_textfield",
                  "operator": "EQUALS",
                  "triggerValue": "true",
                  "effectType": "EDITABLE",
                  "effectValue": true
                }
              ]
            }
        """
    }
}
