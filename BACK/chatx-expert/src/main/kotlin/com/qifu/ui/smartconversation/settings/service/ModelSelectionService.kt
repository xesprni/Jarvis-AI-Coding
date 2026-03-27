package com.qifu.ui.smartconversation.settings.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.qifu.ui.smartconversation.settings.models.ModelRegistry
import com.qifu.ui.smartconversation.settings.models.ModelSelection
import com.qifu.ui.smartconversation.settings.models.ModelSettings
import com.qihoo.finance.lowcode.smartconversation.service.FeatureType
import com.qihoo.finance.lowcode.smartconversation.service.ServiceType

@Service
class ModelSelectionService {

    fun getModelSelectionForFeature(
        featureType: FeatureType
    ): ModelSelection {
        return try {
            val modelSettings = service<ModelSettings>()
            val modelDetailsState = modelSettings.state.getModelSelection(featureType)
            
            if (modelDetailsState != null && modelDetailsState.model != null && modelDetailsState.provider != null) {
                val foundModel = service<ModelRegistry>().findModel(modelDetailsState.provider!!, modelDetailsState.model!!)
                if (foundModel != null) {
                    return foundModel
                }
            }
            
            service<ModelRegistry>().getDefaultModelForFeature(featureType)
        } catch (exception: Exception) {
            logger.warn(
                "Error getting model selection for feature: $featureType, using default",
                exception
            )
            service<ModelRegistry>().getDefaultModelForFeature(featureType)
        }
    }

    fun getServiceForFeature(featureType: FeatureType): ServiceType {
        return try {
            getModelSelectionForFeature(featureType).provider
        } catch (exception: Exception) {
            logger.warn("Error getting service for feature: $featureType, using default", exception)
            ServiceType.PROXYAI
        }
    }

    fun getModelForFeature(featureType: FeatureType): String {
        return try {
            getModelSelectionForFeature(featureType).model
        } catch (exception: Exception) {
            logger.warn("Error getting model for feature: $featureType, using default", exception)
            service<ModelRegistry>().getDefaultModelForFeature(featureType).model
        }
    }

    companion object {

        private val logger = thisLogger()

        @JvmStatic
        fun getInstance(): ModelSelectionService {
            return ApplicationManager.getApplication().getService(ModelSelectionService::class.java)
        }
    }
}