'use client'

import { useTranslation } from '@/lib/i18n/client'
import '@/lib/i18n/config'

export default function HomePage() {
  const { t } = useTranslation('common')

  return (
    <main className="flex min-h-screen flex-col items-center justify-center gap-4 p-8">
      <h1 className="text-4xl font-bold text-phenoai-600">
        {t('app.name')}
      </h1>
      <p className="text-lg text-phenoai-500">
        {t('app.tagline')}
      </p>
    </main>
  )
}
