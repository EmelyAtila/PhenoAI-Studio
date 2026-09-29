import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import LanguageDetector from 'i18next-browser-languagedetector'

import ptBR from '@/locales/pt-BR/common.json'
import enUS from '@/locales/en-US/common.json'

i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources: {
      'pt-BR': { common: ptBR },
      'en-US': { common: enUS },
    },
    defaultNS: 'common',
    fallbackLng: 'pt-BR',
    supportedLngs: ['pt-BR', 'en-US'],
    detection: {
      order: ['localStorage', 'navigator'],
      caches: ['localStorage'],
    },
    interpolation: {
      escapeValue: false,
    },
  })

export default i18n
