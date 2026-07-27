import {
  Alert,
  Box,
  Button,
  Checkbox,
  FormControlLabel,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Rnd } from 'react-rnd'
import { getBadgeTemplate, saveBadgeTemplate, type BadgeTemplate, type ElementStyle } from '../../api/crew'
import { extractErrorMessage } from '../../api/client'

// Editor preview scale — matches the physical badge's 10cm x 15cm (2:3) aspect ratio. The
// actual print output uses cm/pt directly (ExhibitorPassesPage) and is unaffected by this
// on-screen scale; only the stored xPercent/yPercent/fontSizePt travel between the two.
const ARTBOARD_WIDTH = 300
const ARTBOARD_HEIGHT = 450
const ELEMENT_WIDTH = 220
const ELEMENT_HEIGHT = 40

type ElementKey = 'name' | 'designation' | 'company'

const ELEMENT_LABELS: Record<ElementKey, string> = {
  name: 'Name',
  designation: 'Designation',
  company: 'Company',
}

const SAMPLE_TEXT: Record<ElementKey, string> = {
  name: 'Jane Doe',
  designation: 'Manager',
  company: 'Acme Corp',
}

function percentToPixels(style: ElementStyle) {
  return {
    x: (style.xPercent / 100) * ARTBOARD_WIDTH - ELEMENT_WIDTH / 2,
    y: (style.yPercent / 100) * ARTBOARD_HEIGHT - ELEMENT_HEIGHT / 2,
  }
}

function pixelsToPercent(x: number, y: number) {
  return {
    xPercent: ((x + ELEMENT_WIDTH / 2) / ARTBOARD_WIDTH) * 100,
    yPercent: ((y + ELEMENT_HEIGHT / 2) / ARTBOARD_HEIGHT) * 100,
  }
}

export default function BadgeTemplateEditorPage() {
  const queryClient = useQueryClient()
  const query = useQuery({ queryKey: ['badge-template'], queryFn: getBadgeTemplate })
  const [template, setTemplate] = useState<BadgeTemplate | null>(null)
  const [loadedFrom, setLoadedFrom] = useState<BadgeTemplate | undefined>(undefined)

  // Seed local editable state from the fetched template exactly once it arrives (or changes
  // identity, e.g. after a save) — done during render, not an effect, per React's own guidance
  // for "adjust state when a prop changes" rather than syncing via a synchronous setState-in-effect.
  if (query.data && query.data !== loadedFrom) {
    setLoadedFrom(query.data)
    setTemplate(query.data)
  }

  const mutation = useMutation({
    mutationFn: (values: BadgeTemplate) => saveBadgeTemplate(values),
    onSuccess: (saved) => {
      setTemplate(saved)
      queryClient.setQueryData(['badge-template'], saved)
    },
  })

  const updateElement = (key: ElementKey, patch: Partial<ElementStyle>) => {
    setTemplate((current) => (current ? { ...current, [key]: { ...current[key], ...patch } } : current))
  }

  if (!template) {
    return (
      <Typography variant="h5" gutterBottom>
        Badge Template
      </Typography>
    )
  }

  return (
    <>
      <Typography variant="h5" gutterBottom>
        Badge Template
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Drag Name, Designation, and Company into position for the 10cm × 15cm printed badge.
        This layout applies to every exhibitor badge once saved.
      </Typography>

      {mutation.isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {extractErrorMessage(mutation.error)}
        </Alert>
      )}

      <Stack direction={{ xs: 'column', md: 'row' }} spacing={4}>
        <Paper
          sx={{
            position: 'relative',
            width: ARTBOARD_WIDTH,
            height: ARTBOARD_HEIGHT,
            flexShrink: 0,
            bgcolor: 'background.default',
            border: '1px solid',
            borderColor: 'divider',
          }}
        >
          {(Object.keys(ELEMENT_LABELS) as ElementKey[]).map((key) => {
            const style = template[key]
            const { x, y } = percentToPixels(style)
            return (
              <Rnd
                key={key}
                size={{ width: ELEMENT_WIDTH, height: ELEMENT_HEIGHT }}
                position={{ x, y }}
                bounds="parent"
                enableResizing={false}
                onDragStop={(_event, data) => updateElement(key, pixelsToPercent(data.x, data.y))}
              >
                <Box
                  sx={{
                    width: '100%',
                    height: '100%',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    cursor: 'move',
                    border: '1px dashed',
                    borderColor: 'primary.main',
                    bgcolor: 'background.paper',
                    fontSize: `${style.fontSizePt}pt`,
                    fontWeight: style.bold ? 700 : 400,
                    userSelect: 'none',
                  }}
                >
                  {SAMPLE_TEXT[key]}
                </Box>
              </Rnd>
            )
          })}
        </Paper>

        <Stack spacing={3} sx={{ flexGrow: 1, minWidth: 260 }}>
          {(Object.keys(ELEMENT_LABELS) as ElementKey[]).map((key) => (
            <Stack key={key} direction="row" spacing={2} sx={{ alignItems: 'center' }}>
              <Typography sx={{ minWidth: 90 }}>{ELEMENT_LABELS[key]}</Typography>
              <TextField
                label="Font size (pt)"
                type="number"
                size="small"
                value={template[key].fontSizePt}
                onChange={(e) => updateElement(key, { fontSizePt: Number(e.target.value) || 1 })}
                sx={{ width: 130 }}
              />
              <FormControlLabel
                control={
                  <Checkbox
                    checked={template[key].bold}
                    onChange={(e) => updateElement(key, { bold: e.target.checked })}
                  />
                }
                label="Bold"
              />
            </Stack>
          ))}

          <Box>
            <Button variant="contained" disabled={mutation.isPending} onClick={() => mutation.mutate(template)}>
              {mutation.isPending ? 'Saving…' : 'Save Template'}
            </Button>
          </Box>
        </Stack>
      </Stack>
    </>
  )
}
