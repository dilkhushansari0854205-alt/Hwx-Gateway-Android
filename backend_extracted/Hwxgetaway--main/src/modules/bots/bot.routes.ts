import { Router, Request, Response } from 'express';
import { firebaseAuth } from '../../middleware/firebaseAuth.ts';
import { createBotSchema, updateBotSchema } from '../../validators/index.ts';
import { BotService } from './bot.service.ts';

const router = Router();

/**
 * POST /v1/bots
 * Creates a new Bot with device and SIM assignment.
 */
router.post('/', firebaseAuth(), async (req: Request, res: Response): Promise<void> => {
  const validated = createBotSchema.parse(req.body);
  const bot = await BotService.createBot(req.user!.uid, validated);

  res.status(201).json({
    success: true,
    data: bot,
    message: 'Bot created successfully',
  });
});

/**
 * GET /v1/bots
 * Lists all bots owned by the authenticated user.
 */
router.get('/', firebaseAuth(), async (req: Request, res: Response): Promise<void> => {
  const bots = await BotService.listBots(req.user!.uid);

  res.status(200).json({
    success: true,
    data: bots,
  });
});

/**
 * GET /v1/bots/:botId
 * Retrieves specific bot configuration.
 */
router.get('/:botId', firebaseAuth(), async (req: Request, res: Response): Promise<void> => {
  const { botId } = req.params;
  const bot = await BotService.getBot(req.user!.uid, botId);

  if (!bot) {
    res.status(404).json({
      success: false,
      error: { code: 'BOT_NOT_FOUND', message: 'Bot not found' },
    });
    return;
  }

  res.status(200).json({
    success: true,
    data: bot,
  });
});

/**
 * PATCH /v1/bots/:botId
 * Updates bot configuration, webhook, SIM mode, or enabled status.
 */
router.patch('/:botId', firebaseAuth(), async (req: Request, res: Response): Promise<void> => {
  const { botId } = req.params;
  const validated = updateBotSchema.parse(req.body);

  const updatedBot = await BotService.updateBot(req.user!.uid, botId, validated);

  res.status(200).json({
    success: true,
    data: updatedBot,
    message: 'Bot updated successfully',
  });
});

/**
 * DELETE /v1/bots/:botId
 * Deletes a bot.
 */
router.delete('/:botId', firebaseAuth(), async (req: Request, res: Response): Promise<void> => {
  const { botId } = req.params;
  const deleted = await BotService.deleteBot(req.user!.uid, botId);

  if (!deleted) {
    res.status(404).json({
      success: false,
      error: { code: 'BOT_NOT_FOUND', message: 'Bot not found' },
    });
    return;
  }

  res.status(200).json({
    success: true,
    message: 'Bot deleted successfully',
  });
});

export const botRoutes = router;
