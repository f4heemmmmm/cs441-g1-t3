import { NestFactory } from '@nestjs/core';
import { NestExpressApplication } from '@nestjs/platform-express';
import { join } from 'path';
import { AppModule } from './app.module';

async function bootstrap() {
  const app = await NestFactory.create<NestExpressApplication>(AppModule);

  // Handlebars view engine
  app.setBaseViewsDir(join(__dirname, 'views'));
  app.setViewEngine('hbs');

  // Static assets
  app.useStaticAssets(join(__dirname, '..', 'public'));

  app.enableCors();

  await app.listen(3000);
  console.log('[Dashboard-NestJS] Running on http://localhost:3000');
}
bootstrap();
